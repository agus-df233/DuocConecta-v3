package cl.duoc.duocconecta.bff;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.bff.dto.Autor;
import cl.duoc.duocconecta.bff.dto.PromptRespuesta;
import cl.duoc.duocconecta.bff.service.ClientePrompts;
import cl.duoc.duocconecta.bff.service.ClienteUsuarios;
import java.time.Instant;
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

/** La biblioteca llega al frontend con el autor de cada prompt y el del original de una copia. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BibliotecaBffTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientePrompts clientePrompts;

    @MockitoBean
    private ClienteUsuarios clienteUsuarios;

    @Test
    @DisplayName("Cada autor se pide una vez, y la copia trae el nombre de quien hizo el original")
    void autoresResueltos() throws Exception {
        UUID original = UUID.randomUUID();
        Instant ahora = Instant.now();
        when(clientePrompts.buscar(anyMap())).thenReturn(List.of(
                prompt(UUID.randomUUID(), "oid-beto", new PromptRespuesta.Origen(original, "Original", "oid-ana", null), ahora),
                prompt(original, "oid-ana", null, ahora)));
        when(clienteUsuarios.buscarAutorPorOid("oid-ana")).thenReturn(new Autor("Ana", "Informática", "Viña del Mar"));
        when(clienteUsuarios.buscarAutorPorOid("oid-beto")).thenReturn(new Autor("Beto", "Diseño", "Viña del Mar"));

        mockMvc.perform(get("/api/v1/bff/biblioteca").param("categoria", "CODIGO")
                        .with(jwt().jwt(t -> t.claim("oid", "oid-x").claim("email", "x@duocuc.cl"))
                                .authorities(List.of((GrantedAuthority) new SimpleGrantedAuthority("ROLE_ESTUDIANTE")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].autor.nombre").value("Beto"))
                .andExpect(jsonPath("$[0].origen.autor.nombre").value("Ana"))
                .andExpect(jsonPath("$[1].autor.nombre").value("Ana"));
        verify(clienteUsuarios, times(1)).buscarAutorPorOid("oid-ana");
    }

    private static PromptRespuesta prompt(UUID id, String autorId, PromptRespuesta.Origen origen, Instant ahora) {
        return new PromptRespuesta(id, "Título", "Propósito", "Contenido", "CODIGO", null, "ChatGPT", autorId, null,
                "PUBLICO", origen, 0, 0, 0, null, false, null, ahora, ahora);
    }
}
