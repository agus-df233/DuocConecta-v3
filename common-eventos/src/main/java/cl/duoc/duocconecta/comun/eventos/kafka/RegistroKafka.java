package cl.duoc.duocconecta.comun.eventos.kafka;

/**
 * Un registro listo para enviar: a qué tópico, con qué clave y qué DTO.
 *
 * @param claveTopico {@code interacciones} o {@code actividad} (ver {@link PropiedadesKafka})
 * @param clave       clave del registro: decide la partición y, con eso, el orden
 * @param valor       el DTO
 * @param tipo        el tipo del DTO, que también viaja como cabecera
 */
public record RegistroKafka(String claveTopico, String clave, Object valor, String tipo) {

    public static final String INTERACCIONES = "interacciones";
    public static final String ACTIVIDAD = "actividad";

    /** Registro del tópico de interacciones, con el recurso como clave. */
    public static RegistroKafka de(InteraccionContenido interaccion) {
        return new RegistroKafka(INTERACCIONES, interaccion.recursoId(), interaccion, interaccion.tipo().name());
    }

    /** Registro del tópico de actividad, con la persona como clave. */
    public static RegistroKafka de(ActividadUsuario actividad) {
        return new RegistroKafka(ACTIVIDAD, actividad.usuarioId(), actividad, actividad.tipo().name());
    }
}
