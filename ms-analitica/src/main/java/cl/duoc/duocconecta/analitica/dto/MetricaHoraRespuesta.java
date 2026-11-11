package cl.duoc.duocconecta.analitica.dto;

import java.time.Instant;

/**
 * Cuántos registros de un tipo llegaron a un tópico en una hora.
 *
 * @param hora     inicio de la hora
 * @param topico   {@code interacciones} o {@code actividad}
 * @param tipo     tipo del registro
 * @param cantidad cuántos
 */
public record MetricaHoraRespuesta(Instant hora, String topico, String tipo, long cantidad) {
}
