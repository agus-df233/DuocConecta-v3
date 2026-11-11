package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Una alerta de actividad para el administrador, con el nombre de quien la provocó. La arma
 * ms-analitica a partir de Kafka; el BFF le suma la persona.
 *
 * @param id             identificador
 * @param usuarioId      {@code oid} de quien la provocó
 * @param persona        su nombre, carrera y sede (vacío si ocultó su perfil)
 * @param tipo           qué repitió: {@code COMENTARIO}, {@code SOLICITUD_COLABORACION}…
 * @param cantidad       cuántas veces
 * @param ventanaMinutos en cuántos minutos
 * @param recursoId      el último proyecto o prompt sobre el que actuó
 * @param desde          inicio de la ráfaga
 * @param hasta          última acción
 * @param creadaEn       cuándo se detectó
 * @param revisada       si el administrador ya la miró
 */
@Schema(description = "Alerta de actividad con la persona resuelta")
public record AlertaActividadRespuesta(UUID id, String usuarioId, Autor persona, String tipo, int cantidad,
                                       int ventanaMinutos, String recursoId, Instant desde, Instant hasta,
                                       Instant creadaEn, boolean revisada) {

    /** Lo que responde ms-analitica, sin la persona. */
    public record DeAnalitica(UUID id, String usuarioId, String tipo, int cantidad, int ventanaMinutos,
                              String recursoId, Instant desde, Instant hasta, Instant creadaEn, boolean revisada) {

        public AlertaActividadRespuesta con(Autor persona) {
            return new AlertaActividadRespuesta(id, usuarioId, persona, tipo, cantidad, ventanaMinutos, recursoId,
                    desde, hasta, creadaEn, revisada);
        }
    }
}
