package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.List;

/**
 * Un consumidor conectado a un grupo.
 *
 * @param id          identificador que le dio el coordinador
 * @param cliente     {@code client.id} (en DuocConecta, el grupo más el número de hilo)
 * @param host        desde dónde se conecta
 * @param particiones particiones que tiene asignadas
 */
public record MiembroGrupoRespuesta(String id, String cliente, String host, List<String> particiones) {
}
