package cl.duoc.duocconecta.analitica.dto;

import java.util.List;

/**
 * Estado de un consumidor de Kafka de este servicio.
 *
 * @param id          identificador del listener
 * @param grupo       grupo de consumidores
 * @param topicos     qué tópicos lee
 * @param corriendo   si el contenedor está encendido
 * @param pausado     si está en pausa: sigue en el grupo, pero no lee (el lag sube)
 * @param particiones particiones que tiene asignadas en esta instancia
 */
public record EstadoConsumidor(String id, String grupo, List<String> topicos, boolean corriendo, boolean pausado,
                               List<String> particiones) {
}
