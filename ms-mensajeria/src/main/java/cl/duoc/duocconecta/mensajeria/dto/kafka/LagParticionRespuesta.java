package cl.duoc.duocconecta.mensajeria.dto.kafka;

/**
 * Dónde va un grupo en una partición.
 *
 * @param topico      tópico
 * @param particion   partición
 * @param confirmado  último offset que el grupo confirmó como procesado
 * @param offsetFinal offset final de la partición (lo último que se escribió + 1)
 * @param lag         cuántos registros le faltan por leer: {@code final - confirmado}
 * @param consumidor  quién la atiende ahora; vacío si el grupo no está conectado
 */
public record LagParticionRespuesta(String topico, int particion, long confirmado, long offsetFinal, long lag,
                                    String consumidor) {
}
