package cl.duoc.duocconecta.comun.eventos.kafka;

/**
 * Lo que le pasó a un proyecto o a un prompt.
 *
 * <p>Las primeras son señales de interés, que suman a la tendencia. Las últimas cambian el estado
 * del recurso: un oculto o eliminado sale de la tendencia, y uno restaurado vuelve.</p>
 */
public enum TipoInteraccion {
    VISTA,
    COPIA,
    COMENTARIO,
    SOLICITUD_COLABORACION,
    COLABORADOR_AGREGADO,
    VALORACION,
    BIFURCACION,
    PUBLICADO,
    OCULTADO,
    RESTAURADO,
    ELIMINADO
}
