package cl.duoc.duocconecta.proyectos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.comun.eventos.PublicadorEventos;
import cl.duoc.duocconecta.comun.eventos.kafka.PublicadorKafka;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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

/** Vistas de un proyecto (v3): una señal que va solo a Kafka y que no cuenta las de la dueña. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VistasTests {

    private static final String DUENA = "ana.vistas@duocuc.cl";
    private static final String OTRA = "beto.vistas@duocuc.cl";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicadorEventos publicador;

    @MockitoBean
    private PublicadorKafka publicadorKafka;

    private String crear(String visibilidad) throws Exception {
        String cuerpo = """
                {
                  "nombre": "Sensor de aforo",
                  "resumen": "Cuenta cuántas personas hay en la biblioteca.",
                  "descripcion": "Proyecto de prueba.",
                  "sede": "Viña del Mar",
                  "herramientas": ["Arduino"],
                  "estado": "EN_DESARROLLO",
                  "visibilidad": "%s",
                  "colaboradoresIds": []
                }""".formatted(visibilidad);
        String respuesta = mockMvc.perform(post("/api/v1/proyectos").with(estudiante(DUENA))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    @Test
    @DisplayName("Otra persona abre el proyecto: 204 y la vista sale a Kafka con su oid")
    void vistaDeOtraPersona() throws Exception {
        String id = crear("PUBLICO");

        mockMvc.perform(post("/api/v1/proyectos/{id}/vistas", id).with(estudiante(OTRA)))
                .andExpect(status().isNoContent());

        verify(publicadorKafka).publicarUso(TipoInteraccion.VISTA, TipoActividad.VISTA, TipoRecurso.PROYECTO,
                id, "oid-" + OTRA);
    }

    @Test
    @DisplayName("La dueña abre su propio proyecto: 204, pero no cuenta")
    void vistaDeLaDuena() throws Exception {
        String id = crear("PUBLICO");

        mockMvc.perform(post("/api/v1/proyectos/{id}/vistas", id).with(estudiante(DUENA)))
                .andExpect(status().isNoContent());

        verify(publicadorKafka, never()).publicarUso(any(), any(), any(), eq(id), any());
    }

    @Test
    @DisplayName("Un proyecto privado ajeno: 403, igual que al abrirlo")
    void privadoAjeno() throws Exception {
        String id = crear("PRIVADO");

        mockMvc.perform(post("/api/v1/proyectos/{id}/vistas", id).with(estudiante(OTRA)))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor estudiante(String correo) {
        return jwt().jwt(t -> t.claim("oid", "oid-" + correo).claim("email", correo).claim("name", correo))
                .authorities(List.of((GrantedAuthority) new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
    }
}
