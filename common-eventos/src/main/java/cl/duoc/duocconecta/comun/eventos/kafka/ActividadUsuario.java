package cl.duoc.duocconecta.comun.eventos.kafka;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Registro del tópico {@code duocconecta.usuarios.actividad}: algo que hizo una persona. La clave
 * del registro es {@code usuarioId}, así las acciones de cada persona llegan en orden a un solo
 * consumidor, que es lo que permite contarlas en una ventana de tiempo.
 *
 * <p>Lleva el {@code oid} y nada más de la persona: nunca nombre, correo, teléfono ni redes. Por
 * eso el tópico se guarda solo 24 h.</p>
 *
 * @param idEvento    UUID del hecho, para descartar duplicados en el consumidor
 * @param tipo        qué hizo: comentar, pedir colaboración, valorar…
 * @param usuarioId   {@code oid} de quien actuó; es la clave del registro
 * @param tipoRecurso sobre qué tipo de contenido actuó; nulo si no fue sobre un contenido (una
 *                    solicitud de contacto directa a una persona)
 * @param recursoId   sobre qué proyecto o prompt, para que una alerta diga dónde mirar; nulo en
 *                    el mismo caso
 * @param ocurridoEn  cuándo pasó: la ventana de detección cuenta con esta hora
 * @param origen      microservicio que lo publicó
 * @param version     versión del contrato
 */
public record ActividadUsuario(
        @NotBlank String idEvento,
        @NotNull TipoActividad tipo,
        @NotBlank String usuarioId,
        TipoRecurso tipoRecurso,
        String recursoId,
        @NotNull Instant ocurridoEn,
        @NotBlank String origen,
        @Min(1) @Max(VersionesKafka.ACTUAL) int version) {
}
