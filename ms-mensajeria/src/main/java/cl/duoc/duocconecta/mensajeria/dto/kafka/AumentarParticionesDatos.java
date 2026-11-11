package cl.duoc.duocconecta.mensajeria.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Nuevo total de particiones. Kafka solo permite aumentar: bajar exigiría mover registros.
 *
 * @param total cantidad final de particiones
 */
public record AumentarParticionesDatos(@Min(2) @Max(50) @Schema(example = "6") int total) {
}
