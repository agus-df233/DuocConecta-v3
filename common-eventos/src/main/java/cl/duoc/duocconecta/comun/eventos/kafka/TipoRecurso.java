package cl.duoc.duocconecta.comun.eventos.kafka;

/** Qué tipo de contenido recibió la interacción. Va en el DTO porque la clave sola no lo dice. */
public enum TipoRecurso {
    PROYECTO,
    PROMPT
}
