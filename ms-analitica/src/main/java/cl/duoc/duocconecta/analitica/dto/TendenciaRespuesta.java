package cl.duoc.duocconecta.analitica.dto;

import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import java.util.Map;

/**
 * Cuánto interés tuvo un recurso en la ventana de tendencia. Solo identificadores y números: el
 * frontend cruza el {@code recursoId} con lo que ya puede ver, así un proyecto privado nunca se
 * asoma por aquí.
 *
 * @param tipoRecurso proyecto o prompt; nulo si el recurso no tuvo interacciones
 * @param recursoId   su identificador
 * @param puntaje     suma de las interacciones por su peso; 0 si está oculto o eliminado
 * @param conteos     cuántas hubo de cada tipo
 */
public record TendenciaRespuesta(TipoRecurso tipoRecurso, String recursoId, long puntaje,
                                 Map<TipoInteraccion, Long> conteos) {
}
