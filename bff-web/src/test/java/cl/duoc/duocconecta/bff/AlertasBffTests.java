package cl.duoc.duocconecta.bff;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.bff.dto.AlertaActividadRespuesta;
import cl.duoc.duocconecta.bff.dto.Autor;
import cl.duoc.duocconecta.bff.service.ClienteAnalitica;
import cl.duoc.duocconecta.bff.service.ClienteUsuarios;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Las alertas de Kafka llegan al administrador con el nombre de cada persona, pedido una vez. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertasBffTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteAnalitica clienteAnalitica;

    @MockitoBean
    private ClienteUsuarios clienteUsuarios;

    @Test
    @DisplayName("Dos alertas de la misma persona: su nombre se pide una sola vez")
    void alertasConPersona() throws Exception {
        Instant ahora = Instant.now();
        when(clienteAnalitica.alertas(false)).thenReturn(List.of(
                new AlertaActividadRespuesta.DeAnalitica(UUID.randomUUID(), "oid-ana", "COMENTARIO", 9, 5, "p-1",
                        ahora, ahora, ahora, false),
                new AlertaActividadRespuesta.DeAnalitica(UUID.randomUUID(), "oid-ana", "VALORACION", 11, 5, "q-1",
                        ahora, ahora, ahora, true)));
        when(clienteUsuarios.buscarAutorPorOid("oid-ana")).thenReturn(new Autor("Ana", "Informática", "Viña del Mar"));

        mockMvc.perform(get("/api/v1/bff/admin/alertas").with(token("ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].persona.nombre").value("Ana"))
                .andExpect(jsonPath("$[0].tipo").value("COMENTARIO"))
                .andExpect(jsonPath("$[1].revisada").value(true));
        verify(clienteUsuarios, times(1)).buscarAutorPorOid("oid-ana");
    }

    @Test
    @DisplayName("Sin rol ADMINISTRADOR: 403")
    void soloAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/bff/admin/alertas").with(token("ROLE_ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor token(String... roles) {
        return jwt().jwt(t -> t.claim("oid", "oid-admin").claim("email", "admin@duocuc.cl").claim("name", "Admin"))
                .authorities(Arrays.stream(roles).map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList());
    }
}
