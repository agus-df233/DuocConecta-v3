package cl.duoc.duocconecta.prompts.dto;

import cl.duoc.duocconecta.prompts.domain.CategoriaPrompt;
import cl.duoc.duocconecta.prompts.domain.Visibilidad;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un prompt nuevo o editado.
 *
 * @param titulo      cómo se llama
 * @param proposito   para qué sirve, en una o dos líneas
 * @param contenido   el texto del prompt
 * @param categoria   tipo de tarea
 * @param carrera     para qué carrera se pensó (opcional)
 * @param herramienta con qué IA se probó (opcional)
 * @param visibilidad público o borrador privado
 */
public record PromptDatos(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título admite hasta 150 caracteres")
        @Schema(example = "Explícame un error de compilación") String titulo,
        @NotBlank(message = "Cuenta para qué sirve")
        @Size(max = 500, message = "El propósito admite hasta 500 caracteres")
        @Schema(example = "Pega el error y el código: te dice qué pasa y cómo arreglarlo, sin hacerte la tarea.") String proposito,
        @NotBlank(message = "El texto del prompt es obligatorio")
        @Size(max = 8000, message = "El prompt admite hasta 8000 caracteres") String contenido,
        @NotNull(message = "Elige una categoría") CategoriaPrompt categoria,
        @Size(max = 120) String carrera,
        @Size(max = 80) @Schema(example = "ChatGPT") String herramienta,
        @NotNull(message = "Elige si es público o privado") Visibilidad visibilidad) {
}
