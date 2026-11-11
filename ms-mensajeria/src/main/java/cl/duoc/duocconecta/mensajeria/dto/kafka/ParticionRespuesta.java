package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.List;

/**
 * Una partición de un tópico.
 *
 * @param numero        número de partición
 * @param lider         broker que atiende lecturas y escrituras; -1 si no tiene (sin réplicas vivas)
 * @param replicas      brokers que guardan una copia
 * @param sincronizadas réplicas al día (ISR). Si bajan de {@code min.insync.replicas}, no se escribe
 * @param offsetInicial el registro más antiguo que la retención todavía conserva
 * @param offsetFinal   el próximo offset a escribir: {@code final - inicial} = registros guardados
 */
public record ParticionRespuesta(int numero, int lider, List<Integer> replicas, List<Integer> sincronizadas,
                                 long offsetInicial, long offsetFinal) {
}
