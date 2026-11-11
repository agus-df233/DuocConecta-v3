package cl.duoc.duocconecta.prompts.dto;

import java.util.List;

/**
 * Cifras de la biblioteca para el dashboard.
 *
 * @param total          prompts publicados en total
 * @param publicos       los que se ven en la biblioteca (sin contar ocultos)
 * @param ocultos        ocultos por moderación
 * @param valoraciones   valoraciones dadas
 * @param porCategoria   cuántos por categoría
 * @param porCarrera     cuántos por carrera
 */
public record EstadisticasPromptsRespuesta(long total, long publicos, long ocultos, long valoraciones,
                                           List<Conteo> porCategoria, List<Conteo> porCarrera) {
}
