package cl.duoc.duocconecta.prompts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.comun.eventos.PublicadorEventos;
import cl.duoc.duocconecta.comun.eventos.kafka.PublicadorKafka;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** La biblioteca de prompts: publicar, buscar, bifurcar, valorar, moderar y quién puede qué. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BibliotecaTests {

    private static final String ANA = "ana.prompts@duocuc.cl";
    private static final String BETO = "beto.prompts@duocuc.cl";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicadorEventos publicador;

    @MockitoBean
    private PublicadorKafka publicadorKafka;

    private String crear(String correo, String titulo, String visibilidad) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/prompts").with(estudiante(correo))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"titulo":"%s","proposito":"Para probar.","contenido":"Haz esto y aquello.",
                                 "categoria":"CODIGO","carrera":"Ingeniería en Informática","herramienta":"ChatGPT",
                                 "visibilidad":"%s"}""".formatted(titulo, visibilidad)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @DisplayName("Publicar y encontrarlo con filtros; un borrador ajeno no aparece y responde 404")
    void publicarYBuscar() throws Exception {
        String publico = crear(ANA, "Depurar con pistas", "PUBLICO");
        String borrador = crear(ANA, "Borrador secreto", "PRIVADO");

        mockMvc.perform(get("/api/v1/prompts").param("texto", "depurar").param("categoria", "CODIGO")
                        .with(estudiante(BETO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", Matchers.hasItem(publico)))
                .andExpect(jsonPath("$[*].id", Matchers.not(Matchers.hasItem(borrador))));
        mockMvc.perform(get("/api/v1/prompts/{id}", borrador).with(estudiante(BETO)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/prompts/mios").with(estudiante(ANA)))
                .andExpect(jsonPath("$[*].id", Matchers.hasItems(publico, borrador)));
    }

    @Test
    @DisplayName("Solo el autor edita y borra: otra persona recibe 403")
    void soloElAutor() throws Exception {
        String id = crear(ANA, "Mío", "PUBLICO");
        mockMvc.perform(put("/api/v1/prompts/{id}", id).with(estudiante(BETO)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Ajeno","proposito":"x","contenido":"x","categoria":"OTRO","visibilidad":"PUBLICO"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/prompts/{id}", id).with(estudiante(BETO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/prompts/{id}", id).with(estudiante(ANA)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Bifurcar crea una copia privada que recuerda el original; el propio no se bifurca")
    void bifurcar() throws Exception {
        String original = crear(ANA, "Original", "PUBLICO");

        String respuesta = mockMvc.perform(post("/api/v1/prompts/{id}/bifurcaciones", original).with(estudiante(BETO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibilidad").value("PRIVADO"))
                .andExpect(jsonPath("$.origen.id").value(original))
                .andExpect(jsonPath("$.autorId").value("oid-" + BETO))
                .andReturn().getResponse().getContentAsString();
        String copia = JsonPath.read(respuesta, "$.id");

        mockMvc.perform(get("/api/v1/prompts/{id}", original).with(estudiante(ANA)))
                .andExpect(jsonPath("$.bifurcaciones").value(1));
        mockMvc.perform(post("/api/v1/prompts/{id}/bifurcaciones", original).with(estudiante(ANA)))
                .andExpect(status().isBadRequest());

        // Si el original se borra, la copia sigue y solo pierde el enlace.
        mockMvc.perform(delete("/api/v1/prompts/{id}", original).with(estudiante(ANA))).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/prompts/{id}", copia).with(estudiante(BETO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").doesNotExist());
    }

    @Test
    @DisplayName("Una valoración por persona: valorar de nuevo reemplaza; el propio responde 400")
    void valorar() throws Exception {
        String id = crear(ANA, "Para valorar", "PUBLICO");

        mockMvc.perform(put("/api/v1/prompts/{id}/valoracion", id).with(estudiante(BETO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promedio").value(5.0));
        mockMvc.perform(put("/api/v1/prompts/{id}/valoracion", id).with(estudiante(BETO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":3}"))
                .andExpect(jsonPath("$.valoraciones").value(1))
                .andExpect(jsonPath("$.promedio").value(3.0))
                .andExpect(jsonPath("$.miValoracion").value(3));
        mockMvc.perform(put("/api/v1/prompts/{id}/valoracion", id).with(estudiante(ANA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/prompts/{id}/valoracion", id).with(estudiante(BETO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Abrir y copiar van a Kafka; los del autor no cuentan")
    void senalesDeUso() throws Exception {
        String id = crear(ANA, "Para copiar", "PUBLICO");

        mockMvc.perform(post("/api/v1/prompts/{id}/copias", id).with(estudiante(BETO))).andExpect(status().isNoContent());
        verify(publicadorKafka).publicarUso(TipoInteraccion.COPIA, TipoActividad.COPIA, TipoRecurso.PROMPT, id, "oid-" + BETO);

        mockMvc.perform(post("/api/v1/prompts/{id}/vistas", id).with(estudiante(ANA))).andExpect(status().isNoContent());
        verify(publicadorKafka, never()).publicarUso(eq(TipoInteraccion.VISTA), any(), any(), eq(id), any());
    }

    @Test
    @DisplayName("Moderación: el oculto sale de la biblioteca y no se bifurca; solo ADMINISTRADOR")
    void moderacion() throws Exception {
        String id = crear(ANA, "Para moderar", "PUBLICO");

        mockMvc.perform(patch("/api/v1/prompts/admin/{id}/ocultar", id).with(estudiante(BETO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"spam\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/prompts/admin/{id}/ocultar", id).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oculto").value(true));
        mockMvc.perform(patch("/api/v1/prompts/admin/{id}/ocultar", id).with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"otra vez\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/prompts/{id}/bifurcaciones", id).with(estudiante(BETO)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/prompts/{id}", id).with(estudiante(ANA)))
                .andExpect(jsonPath("$.motivoOcultamiento").value("spam"));
        mockMvc.perform(get("/api/v1/prompts/admin/estadisticas").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocultos").value(Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Sin token: 401")
    void sinToken() throws Exception {
        mockMvc.perform(get("/api/v1/prompts")).andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor estudiante(String correo) {
        return token(correo, "ROLE_ESTUDIANTE");
    }

    private static RequestPostProcessor administrador() {
        return token("admin.prompts@duocuc.cl", "ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor token(String correo, String... roles) {
        return jwt().jwt(t -> t.claim("oid", "oid-" + correo).claim("email", correo).claim("name", correo))
                .authorities(Arrays.stream(roles).map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList());
    }
}
