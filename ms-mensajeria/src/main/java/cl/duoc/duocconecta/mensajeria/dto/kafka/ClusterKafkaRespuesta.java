package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.List;

/**
 * El clúster de Kafka.
 *
 * @param idCluster    identificador del clúster
 * @param controlador  {@code broker.id} del controlador
 * @param brokers      los brokers vivos
 */
public record ClusterKafkaRespuesta(String idCluster, int controlador, List<BrokerRespuesta> brokers) {
}
