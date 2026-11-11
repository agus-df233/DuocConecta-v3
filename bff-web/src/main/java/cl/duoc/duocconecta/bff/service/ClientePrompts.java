package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.PromptRespuesta;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Cliente hacia ms-prompts. Propaga el token: ms-prompts decide qué ve cada persona. */
@Service
public class ClientePrompts {

    private static final ParameterizedTypeReference<List<PromptRespuesta>> LISTA = new ParameterizedTypeReference<>() {};

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public ClientePrompts(RestClient clienteMsPrompts, UsuarioActual usuarioActual) {
        this.cliente = clienteMsPrompts;
        this.usuarioActual = usuarioActual;
    }

    /** Busca con los mismos filtros que recibe el BFF; los vacíos no se envían. */
    public List<PromptRespuesta> buscar(Map<String, String> filtros) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/v1/prompts");
        filtros.forEach((clave, valor) -> {
            if (valor != null && !valor.isBlank()) {
                uri.queryParam(clave, valor);
            }
        });
        return cliente.get()
                .uri(uri.encode().build().toUriString())
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(LISTA);
    }

    /** Los prompts de una persona, ocultos y borradores incluidos (moderación, solo ADMINISTRADOR). */
    public List<PromptRespuesta> deAutor(String oid) {
        return cliente.get()
                .uri("/api/v1/prompts/admin?autorId={oid}", oid)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(LISTA);
    }

    /** El detalle, con las estrellas que le dio quien mira. */
    public PromptRespuesta obtener(UUID id) {
        return cliente.get()
                .uri("/api/v1/prompts/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(PromptRespuesta.class);
    }
}
