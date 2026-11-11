package cl.duoc.duocconecta.comun.eventos.kafka;

import static cl.duoc.duocconecta.comun.eventos.TiposDeEvento.*;

import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Convierte un evento de negocio en los registros de Kafka que le corresponden.
 *
 * <p>Es el mismo hecho mirado desde dos lados. En {@code interacciones} cuenta lo que le pasó al
 * contenido (clave: el recurso), y en {@code actividad}, lo que hizo la persona (clave: el
 * {@code oid}). Los dos registros llevan el mismo {@code idEvento}, así se pueden cruzar.</p>
 *
 * <p>Lo que no sirve para la analítica no se traduce: una edición, una respuesta a una solicitud
 * o un comentario borrado no suman a la tendencia ni dicen nada de abuso.</p>
 */
public class TraductorEventosKafka {

    private final String origen;

    public TraductorEventosKafka(String origen) {
        this.origen = origen;
    }

    /** Los registros de un evento de negocio; una lista vacía si no le interesa a Kafka. */
    public List<RegistroKafka> traducir(EventoDuocConecta evento) {
        List<RegistroKafka> registros = new ArrayList<>(2);
        String actor = evento.actorId();
        switch (evento.tipo()) {
            case PROYECTO_PUBLICADO -> {
                interaccion(registros, evento, TipoInteraccion.PUBLICADO, TipoRecurso.PROYECTO, evento.proyectoId());
                actividad(registros, evento, TipoActividad.PUBLICACION, TipoRecurso.PROYECTO, evento.proyectoId());
            }
            case PROYECTO_COMENTADO -> {
                interaccion(registros, evento, TipoInteraccion.COMENTARIO, TipoRecurso.PROYECTO, evento.proyectoId());
                actividad(registros, evento, TipoActividad.COMENTARIO, TipoRecurso.PROYECTO, evento.proyectoId());
            }
            case PROYECTO_COLABORADOR_AGREGADO ->
                    interaccion(registros, evento, TipoInteraccion.COLABORADOR_AGREGADO, TipoRecurso.PROYECTO, evento.proyectoId());
            case PROYECTO_OCULTADO ->
                    interaccion(registros, evento, TipoInteraccion.OCULTADO, TipoRecurso.PROYECTO, evento.proyectoId());
            case PROYECTO_RESTAURADO ->
                    interaccion(registros, evento, TipoInteraccion.RESTAURADO, TipoRecurso.PROYECTO, evento.proyectoId());
            case PROYECTO_ELIMINADO ->
                    interaccion(registros, evento, TipoInteraccion.ELIMINADO, TipoRecurso.PROYECTO, evento.proyectoId());
            case COLABORACION_SOLICITADA -> {
                // Una solicitud puede ser directa a una persona, sin proyecto: igual cuenta como actividad.
                interaccion(registros, evento, TipoInteraccion.SOLICITUD_COLABORACION, TipoRecurso.PROYECTO, evento.proyectoId());
                actividad(registros, evento, TipoActividad.SOLICITUD_COLABORACION,
                        evento.proyectoId() == null ? null : TipoRecurso.PROYECTO, evento.proyectoId());
            }
            case PROMPT_PUBLICADO -> {
                interaccion(registros, evento, TipoInteraccion.PUBLICADO, TipoRecurso.PROMPT, evento.recursoId());
                actividad(registros, evento, TipoActividad.PUBLICACION, TipoRecurso.PROMPT, evento.recursoId());
            }
            case PROMPT_BIFURCADO -> {
                // Suma al original, que es el que demostró ser útil.
                interaccion(registros, evento, TipoInteraccion.BIFURCACION, TipoRecurso.PROMPT, evento.recursoId());
                actividad(registros, evento, TipoActividad.BIFURCACION, TipoRecurso.PROMPT, evento.recursoId());
            }
            case PROMPT_VALORADO -> {
                interaccion(registros, evento, TipoInteraccion.VALORACION, TipoRecurso.PROMPT, evento.recursoId());
                actividad(registros, evento, TipoActividad.VALORACION, TipoRecurso.PROMPT, evento.recursoId());
            }
            case PROMPT_OCULTADO ->
                    interaccion(registros, evento, TipoInteraccion.OCULTADO, TipoRecurso.PROMPT, evento.recursoId());
            case PROMPT_RESTAURADO ->
                    interaccion(registros, evento, TipoInteraccion.RESTAURADO, TipoRecurso.PROMPT, evento.recursoId());
            case PROMPT_ELIMINADO ->
                    interaccion(registros, evento, TipoInteraccion.ELIMINADO, TipoRecurso.PROMPT, evento.recursoId());
            default -> {
                // Ediciones, respuestas a solicitudes y comentarios borrados: no le interesan a la analítica.
            }
        }
        if (actor == null || actor.isBlank()) {
            registros.removeIf(r -> RegistroKafka.ACTIVIDAD.equals(r.claveTopico()));
        }
        return registros;
    }

    /**
     * Los registros de una señal de uso que no es un evento de negocio (abrir o copiar): no avisa a
     * nadie, así que va solo a Kafka.
     */
    public List<RegistroKafka> uso(TipoInteraccion tipo, TipoActividad actividad, TipoRecurso tipoRecurso,
                                   String recursoId, String usuarioId, String idEvento,
                                   Instant ocurridoEn) {
        List<RegistroKafka> registros = new ArrayList<>(2);
        registros.add(RegistroKafka.de(new InteraccionContenido(idEvento, tipo, tipoRecurso, recursoId,
                ocurridoEn, origen, VersionesKafka.ACTUAL)));
        if (usuarioId != null && !usuarioId.isBlank()) {
            registros.add(RegistroKafka.de(new ActividadUsuario(idEvento, actividad, usuarioId, tipoRecurso,
                    recursoId, ocurridoEn, origen, VersionesKafka.ACTUAL)));
        }
        return registros;
    }

    private void interaccion(List<RegistroKafka> registros, EventoDuocConecta evento, TipoInteraccion tipo,
                             TipoRecurso tipoRecurso, String recursoId) {
        if (recursoId == null || recursoId.isBlank()) {
            return;
        }
        registros.add(RegistroKafka.de(new InteraccionContenido(evento.idEvento(), tipo, tipoRecurso, recursoId,
                evento.ocurridoEn(), origenDe(evento), VersionesKafka.ACTUAL)));
    }

    private void actividad(List<RegistroKafka> registros, EventoDuocConecta evento, TipoActividad tipo,
                           TipoRecurso tipoRecurso, String recursoId) {
        registros.add(RegistroKafka.de(new ActividadUsuario(evento.idEvento(), tipo, evento.actorId(), tipoRecurso,
                recursoId, evento.ocurridoEn(), origenDe(evento), VersionesKafka.ACTUAL)));
    }

    private String origenDe(EventoDuocConecta evento) {
        return evento.origen() != null ? evento.origen() : origen;
    }
}
