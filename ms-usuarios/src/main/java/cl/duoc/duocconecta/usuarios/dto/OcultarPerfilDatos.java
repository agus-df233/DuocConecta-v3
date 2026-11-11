package cl.duoc.duocconecta.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo para ocultar un perfil por moderación. */
public record OcultarPerfilDatos(
        @NotBlank(message = "Indica el motivo") @Size(max = 500, message = "El motivo admite hasta 500 caracteres")
        String motivo) {
}
