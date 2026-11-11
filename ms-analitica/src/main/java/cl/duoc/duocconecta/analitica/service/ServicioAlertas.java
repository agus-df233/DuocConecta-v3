package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.dto.AlertaRespuesta;
import cl.duoc.duocconecta.analitica.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.analitica.repository.RepositorioAlertas;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lo que ve el administrador de las alertas. */
@Service
public class ServicioAlertas {

    private static final int MAXIMO = 100;

    private final RepositorioAlertas alertas;

    public ServicioAlertas(RepositorioAlertas alertas) {
        this.alertas = alertas;
    }

    /** Las más recientes primero; solo las pendientes si se pide. */
    @Transactional(readOnly = true)
    public List<AlertaRespuesta> listar(boolean soloPendientes) {
        var pagina = PageRequest.of(0, MAXIMO);
        var filas = soloPendientes ? alertas.findByRevisadaOrderByCreadaEnDesc(false, pagina)
                : alertas.findAllByOrderByCreadaEnDesc(pagina);
        return filas.stream().map(AlertaRespuesta::desde).toList();
    }

    /** Cuántas faltan por revisar. */
    @Transactional(readOnly = true)
    public long pendientes() {
        return alertas.countByRevisadaFalse();
    }

    /** El administrador ya la miró. */
    @Transactional
    public AlertaRespuesta marcarRevisada(UUID id) {
        var alerta = alertas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Alerta no encontrada: " + id));
        alerta.marcarRevisada();
        return AlertaRespuesta.desde(alerta);
    }
}
