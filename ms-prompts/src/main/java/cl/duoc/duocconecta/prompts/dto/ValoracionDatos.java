package cl.duoc.duocconecta.prompts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Las estrellas que se le dan a un prompt: de 1 a 5. */
public record ValoracionDatos(
        @Min(value = 1, message = "Mínimo 1 estrella") @Max(value = 5, message = "Máximo 5 estrellas")
        @Schema(example = "5") int estrellas) {
}
