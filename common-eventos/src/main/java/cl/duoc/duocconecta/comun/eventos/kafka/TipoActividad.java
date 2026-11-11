package cl.duoc.duocconecta.comun.eventos.kafka;

/** Lo que hizo una persona. Es lo que mira el detector de abuso. */
public enum TipoActividad {
    VISTA,
    COPIA,
    COMENTARIO,
    SOLICITUD_COLABORACION,
    VALORACION,
    BIFURCACION,
    PUBLICACION
}
