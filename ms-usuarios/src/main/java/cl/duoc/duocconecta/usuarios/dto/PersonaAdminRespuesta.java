package cl.duoc.duocconecta.usuarios.dto;

import cl.duoc.duocconecta.usuarios.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Una persona, como la ve el administrador. Trae el correo institucional (para identificarla),
 * pero nunca el teléfono ni las redes: esos solo se comparten por consentimiento mutuo.
 *
 * @param id                  identificador interno
 * @param oid                 {@code oid} del token; es lo que guardan los demás servicios
 * @param visible             si la persona se muestra en los listados (lo elige ella)
 * @param ocultoPorModeracion si un administrador la sacó de los listados
 * @param motivoModeracion    por qué, si está oculta por moderación
 */
@Schema(description = "Persona de la comunidad, vista de administración (sin datos de contacto)")
public record PersonaAdminRespuesta(UUID id, String oid, String nombre, String correo, String rol, String carrera,
                                    String sede, boolean visible, boolean ocultoPorModeracion,
                                    String motivoModeracion, OffsetDateTime moderadoEn, OffsetDateTime creadoEn) {

    public static PersonaAdminRespuesta desde(Usuario u) {
        return new PersonaAdminRespuesta(u.getId(), u.getOidEntra(), u.getNombre(), u.getCorreo(), u.getRol().name(),
                u.getCarrera(), u.getSede(), u.isVisible(), u.isOcultoPorModeracion(), u.getMotivoModeracion(),
                u.getModeradoEn(), u.getCreadoEn());
    }
}
