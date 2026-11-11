package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.List;
import java.util.Map;

/**
 * Un tópico con sus particiones y su configuración.
 *
 * @param nombre        nombre
 * @param protegido     si es de la topología base (no se borra ni se le cambian las particiones)
 * @param replicacion   copias de cada partición
 * @param particiones   detalle por partición; vacío en el listado
 * @param registros     registros guardados en total (suma de final - inicial)
 * @param configuracion retención, limpieza y lo que se haya fijado en el tópico
 */
public record TopicoKafkaRespuesta(String nombre, boolean protegido, int replicacion,
                                   List<ParticionRespuesta> particiones, long registros,
                                   Map<String, String> configuracion) {
}
