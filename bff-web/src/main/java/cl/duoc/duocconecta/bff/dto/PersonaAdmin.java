package cl.duoc.duocconecta.bff.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Una persona como la devuelve ms-usuarios a la administración: sin teléfono ni redes. */
public record PersonaAdmin(UUID id, String oid, String nombre, String correo, String rol, String carrera, String sede,
                           boolean visible, boolean ocultoPorModeracion, String motivoModeracion,
                           OffsetDateTime moderadoEn, OffsetDateTime creadoEn) {
}
