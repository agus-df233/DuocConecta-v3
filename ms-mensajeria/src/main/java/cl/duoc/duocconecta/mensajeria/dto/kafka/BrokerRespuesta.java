package cl.duoc.duocconecta.mensajeria.dto.kafka;

/**
 * Un broker del clúster.
 *
 * @param id          {@code broker.id}
 * @param host        dirección que anuncia a los clientes
 * @param puerto      puerto del listener de los clientes
 * @param controlador si es el controlador: el que elige líderes cuando un broker cae
 */
public record BrokerRespuesta(int id, String host, int puerto, boolean controlador) {
}
