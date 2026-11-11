package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.List;

/**
 * Un grupo de consumidores.
 *
 * @param id          nombre del grupo
 * @param estado      {@code STABLE}, {@code EMPTY}, {@code PREPARING_REBALANCE}…
 * @param asignador   estrategia de reparto de particiones
 * @param lagTotal    registros pendientes en todas sus particiones
 * @param miembros    consumidores conectados; vacío en el listado
 * @param particiones offsets y lag por partición; vacío en el listado
 */
public record GrupoKafkaRespuesta(String id, String estado, String asignador, long lagTotal,
                                  List<MiembroGrupoRespuesta> miembros, List<LagParticionRespuesta> particiones) {
}
