package cl.duoc.duocconecta.comun.eventos.kafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Registro del tópico {@code duocconecta.contenido.interacciones}: algo le pasó a un proyecto o
 * a un prompt. La clave del registro es {@code recursoId}, así todo lo de un mismo recurso cae en
 * la misma partición y se procesa en orden.
 *
 * <p>No dice quién actuó, a propósito: para saber qué está en tendencia no hace falta, y este
 * tópico se guarda 7 días. Lo que es de cada persona va en {@link ActividadUsuario}, que dura 24 h.</p>
 *
 * @param idEvento    UUID del hecho. El consumidor lo usa para no contar dos veces el mismo
 *                    registro si Kafka lo entrega de nuevo (entrega "al menos una vez")
 * @param tipo        qué pasó: suma a la tendencia o cambia el estado del recurso
 * @param tipoRecurso proyecto o prompt; cada uno tiene sus propios pesos en la tendencia
 * @param recursoId   UUID del proyecto o del prompt; es también la clave del registro
 * @param ocurridoEn  cuándo pasó, no cuándo llegó: las ventanas de 7 días se calculan con esto
 * @param origen      microservicio que lo publicó, para rastrear de dónde vino un dato raro
 * @param version     versión del contrato. El consumidor rechaza una que no conoce en vez de
 *                    interpretarla mal
 */
public record InteraccionContenido(
        @NotBlank String idEvento,
        @NotNull TipoInteraccion tipo,
        @NotNull TipoRecurso tipoRecurso,
        @NotBlank String recursoId,
        @NotNull Instant ocurridoEn,
        @NotBlank String origen,
        @Min(1) @Max(VersionesKafka.ACTUAL) int version) {
}
