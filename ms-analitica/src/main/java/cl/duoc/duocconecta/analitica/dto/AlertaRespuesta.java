package cl.duoc.duocconecta.analitica.dto;

import cl.duoc.duocconecta.analitica.domain.Alerta;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import java.time.Instant;
import java.util.UUID;

/**
 * Una alerta, para el administrador. Trae el {@code oid}; el BFF le suma el nombre.
 *
 * @param id             identificador
 * @param usuarioId      {@code oid} de quien la provocó
 * @param tipo           qué repitió
 * @param cantidad       cuántas veces
 * @param ventanaMinutos en cuántos minutos
 * @param recursoId      el último proyecto o prompt sobre el que actuó
 * @param desde          inicio de la ventana
 * @param hasta          última acción de la ráfaga
 * @param creadaEn       cuándo se detectó
 * @param revisada       si el administrador ya la miró
 */
public record AlertaRespuesta(UUID id, String usuarioId, TipoActividad tipo, int cantidad, int ventanaMinutos,
                              String recursoId, Instant desde, Instant hasta, Instant creadaEn, boolean revisada) {

    public static AlertaRespuesta desde(Alerta a) {
        return new AlertaRespuesta(a.getId(), a.getUsuarioId(), a.getTipo(), a.getCantidad(), a.getVentanaMinutos(),
                a.getRecursoId(), a.getDesde(), a.getHasta(), a.getCreadaEn(), a.isRevisada());
    }
}
