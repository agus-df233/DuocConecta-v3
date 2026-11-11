package cl.duoc.duocconecta.mensajeria.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Un tópico nuevo.
 *
 * @param nombre         con el prefijo {@code duocconecta.}
 * @param particiones    cuántas; más particiones = más consumidores en paralelo por grupo
 * @param replicacion    copias de cada partición; no puede superar la cantidad de brokers
 * @param configuracion  retención, limpieza y demás (ver {@link ReglasKafka#CONFIGURACIONES})
 */
public record CrearTopicoKafkaDatos(
        @NotBlank @Size(max = 249) @Pattern(regexp = ReglasKafka.PATRON,
                message = "debe empezar con 'duocconecta.' y usar minúsculas, números, puntos o guiones")
        @Schema(example = "duocconecta.pruebas.demo") String nombre,
        @Min(1) @Max(50) @Schema(example = "3") int particiones,
        @Min(1) @Max(5) @Schema(example = "3") short replicacion,
        @Schema(example = "{\"retention.ms\": \"86400000\", \"cleanup.policy\": \"delete\"}")
        Map<String, String> configuracion) {
}
