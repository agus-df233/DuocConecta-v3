package cl.duoc.duocconecta.bff;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.bff.dto.PersonaAdmin;
import cl.duoc.duocconecta.bff.service.ClienteAnalitica;
import cl.duoc.duocconecta.bff.service.ClientePrompts;
import cl.duoc.duocconecta.bff.service.ClienteProyectos;
import cl.duoc.duocconecta.bff.service.ClienteUsuarios;
import java.time.OffsetDateTime;
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
import org.springframework.web.client.ResourceAccessException;

/** La ficha de una persona junta cuatro servicios; si uno no responde, llega igual y lo dice. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FichaPersonaBffTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ClienteUsuarios usuarios;
    @MockitoBean private ClienteProyectos proyectos;
    @MockitoBean private ClientePrompts prompts;
    @MockitoBean private ClienteAnalitica analitica;

    @Test
    @DisplayName("Con ms-analitica caído, la ficha trae perfil, proyectos y prompts, y marca las alertas como no disponibles")
    void fichaConUnaParteCaida() throws Exception {
        when(usuarios.personaAdmin("oid-ana")).thenReturn(new PersonaAdmin(UUID.randomUUID(), "oid-ana", "Ana",
                "ana@duocuc.cl", "ESTUDIANTE", "Informática", "Viña del Mar", true, false, null, null, OffsetDateTime.now()));
        when(proyectos.deDuena("oid-ana")).thenReturn(List.of());
        when(prompts.deAutor("oid-ana")).thenReturn(List.of());
        when(analitica.alertasDe("oid-ana")).thenThrow(new ResourceAccessException("sin respuesta"));

        mockMvc.perform(get("/api/v1/bff/admin/personas/oid-ana")
                        .with(jwt().jwt(t -> t.claim("oid", "oid-admin").claim("email", "admin@duocuc.cl"))
                                .authorities(List.of((GrantedAuthority) new SimpleGrantedAuthority("ROLE_ESTUDIANTE"),
                                        new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona.nombre").value("Ana"))
                .andExpect(jsonPath("$.persona.telefono").doesNotExist())
                .andExpect(jsonPath("$.noDisponible[0]").value("alertas"));
    }
}
