package cl.duoc.duocconecta.bff.service;

import cl.duoc.duocconecta.bff.dto.AlertaActividadRespuesta;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Cliente hacia ms-analitica. Propaga el token: ms-analitica también exige ADMINISTRADOR. */
@Service
public class ClienteAnalitica {

    private static final ParameterizedTypeReference<List<AlertaActividadRespuesta.DeAnalitica>> ALERTAS =
            new ParameterizedTypeReference<>() {};

    private final RestClient cliente;
    private final UsuarioActual usuarioActual;

    public ClienteAnalitica(RestClient clienteMsAnalitica, UsuarioActual usuarioActual) {
        this.cliente = clienteMsAnalitica;
        this.usuarioActual = usuarioActual;
    }

    /** Las alertas de una persona. */
    public List<AlertaActividadRespuesta.DeAnalitica> alertasDe(String oid) {
        return cliente.get()
                .uri("/api/v1/analitica/admin/alertas?usuarioId={oid}", oid)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(ALERTAS);
    }

    /** Las alertas, las más recientes primero. */
    public List<AlertaActividadRespuesta.DeAnalitica> alertas(boolean pendientes) {
        return cliente.get()
                .uri("/api/v1/analitica/admin/alertas?pendientes={p}", pendientes)
                .header(HttpHeaders.AUTHORIZATION, usuarioActual.cabeceraAuthorization())
                .retrieve()
                .body(ALERTAS);
    }
}
