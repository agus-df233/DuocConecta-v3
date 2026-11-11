package cl.duoc.duocconecta.usuarios;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Gestión de personas (v3): buscar a toda la comunidad y ocultar un perfil por moderación. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonasAdministracionTests {

    private static final String BASE = "/api/v1/usuarios/admin/personas";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("El administrador la encuentra, la oculta, deja de verse en público y la vuelve a mostrar")
    void ocultarYMostrar() throws Exception {
        String correo = "moderada.personas@duocuc.cl";
        mockMvc.perform(get("/api/v1/usuarios/me").with(token(correo, "ROLE_ESTUDIANTE"))).andExpect(status().isOk());

        String lista = mockMvc.perform(get(BASE).param("texto", "moderada.personas").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].correo").value(correo))
                .andExpect(jsonPath("$.content[0].telefono").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(lista, "$.content[0].id");

        mockMvc.perform(patch(BASE + "/{id}/ocultar", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocultoPorModeracion").value(true));
        mockMvc.perform(patch(BASE + "/{id}/ocultar", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"otra vez\"}"))
                .andExpect(status().isConflict());

        // En lo público ya no existe: ni su perfil ni como autora para el BFF.
        mockMvc.perform(get("/api/v1/usuarios/{id}", id).with(token("otra@duocuc.cl", "ROLE_ESTUDIANTE")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/usuarios/por-oid/{oid}", "oid-" + correo).with(token("otra@duocuc.cl", "ROLE_ESTUDIANTE")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE).param("estado", "MODERADA").with(admin()))
                .andExpect(jsonPath("$.content[*].id", Matchers.hasItem(id)));

        mockMvc.perform(patch(BASE + "/{id}/mostrar", id).with(admin())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/usuarios/{id}", id).with(token("otra@duocuc.cl", "ROLE_ESTUDIANTE")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin rol ADMINISTRADOR: 403")
    void soloAdministrador() throws Exception {
        mockMvc.perform(get(BASE).with(token("ana.personas@duocuc.cl", "ROLE_ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor admin() {
        return token("admin.personas@duocuc.cl", "ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor token(String correo, String... roles) {
        return jwt().jwt(t -> t.claim("oid", "oid-" + correo).claim("email", correo).claim("name", correo))
                .authorities(Arrays.stream(roles).map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList());
    }
}
