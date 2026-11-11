package cl.duoc.duocconecta.comun.eventos.kafka;

/**
 * Alguien abrió o copió un contenido. No es un evento de negocio (nadie recibe un aviso por eso),
 * así que solo llega a Kafka. El servicio la emite con {@code publishEvent(...)} igual que un
 * evento, y no sabe que Kafka existe.
 *
 * @param interaccion lo que le pasó al contenido
 * @param actividad   lo que hizo la persona
 * @param tipoRecurso proyecto o prompt
 * @param recursoId   su identificador
 * @param usuarioId   {@code oid} de quien lo usó
 */
public record SenalDeUso(TipoInteraccion interaccion, TipoActividad actividad, TipoRecurso tipoRecurso,
                         String recursoId, String usuarioId) {

    /** Alguien abrió el detalle. */
    public static SenalDeUso vista(TipoRecurso tipoRecurso, String recursoId, String usuarioId) {
        return new SenalDeUso(TipoInteraccion.VISTA, TipoActividad.VISTA, tipoRecurso, recursoId, usuarioId);
    }

    /** Alguien copió el texto de un prompt: la señal de uso más fuerte. */
    public static SenalDeUso copia(String promptId, String usuarioId) {
        return new SenalDeUso(TipoInteraccion.COPIA, TipoActividad.COPIA, TipoRecurso.PROMPT, promptId, usuarioId);
    }
}
