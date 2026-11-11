package cl.duoc.duocconecta.prompts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo para ocultar un prompt. Lo ve su autor. */
public record OcultarPromptDatos(
        @NotBlank(message = "Indica el motivo") @Size(max = 500, message = "El motivo admite hasta 500 caracteres")
        String motivo) {
}
