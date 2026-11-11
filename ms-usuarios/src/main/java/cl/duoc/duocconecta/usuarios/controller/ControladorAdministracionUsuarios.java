package cl.duoc.duocconecta.usuarios.controller;

import cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso;
import cl.duoc.duocconecta.usuarios.dto.EstadisticasUsuariosRespuesta;
import cl.duoc.duocconecta.usuarios.service.ServicioEstadisticas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import cl.duoc.duocconecta.comun.seguridad.Rol;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import cl.duoc.duocconecta.usuarios.dto.OcultarPerfilDatos;
import cl.duoc.duocconecta.usuarios.dto.PersonaAdminRespuesta;
import cl.duoc.duocconecta.usuarios.service.ServicioAdministracionPersonas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que la administración necesita de ms-usuarios. Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/usuarios/admin")
@PreAuthorize(ReglasDeAcceso.SOLO_ADMINISTRADOR)
@Validated
@Tag(name = "Administración", description = "Cifras de la comunidad y gestión de personas")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorAdministracionUsuarios {

    private final ServicioEstadisticas estadisticas;
    private final ServicioAdministracionPersonas personas;
    private final UsuarioActual usuarioActual;

    public ControladorAdministracionUsuarios(ServicioEstadisticas estadisticas, ServicioAdministracionPersonas personas,
                                             UsuarioActual usuarioActual) {
        this.estadisticas = estadisticas;
        this.personas = personas;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Toda la comunidad, también los perfiles ocultos, con búsqueda y filtros. Trae el correo para
     * identificar a la persona, nunca el teléfono ni las redes.
     */
    @Operation(summary = "Busca personas (administración)",
            description = "texto busca en nombre y correo; estado: VISIBLE, OCULTA (por la persona) o MODERADA.")
    @GetMapping("/personas")
    public Page<PersonaAdminRespuesta> personas(@RequestParam(required = false) String texto,
                                                @RequestParam(required = false) String carrera,
                                                @RequestParam(required = false) Rol rol,
                                                @RequestParam(required = false) String estado,
                                                @RequestParam(defaultValue = "0") @Min(0) int pagina,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return personas.buscar(texto, carrera, rol, estado, PageRequest.of(pagina, tamano));
    }

    /** Una persona por su {@code oid}, también si está oculta. */
    @Operation(summary = "Una persona por su oid (administración)")
    @GetMapping("/personas/por-oid/{oid}")
    public PersonaAdminRespuesta porOid(@PathVariable String oid) {
        return personas.porOid(oid);
    }

    /** La saca de los listados públicos por moderación. 409 si ya lo estaba. */
    @Operation(summary = "Oculta un perfil por moderación",
            description = "No borra nada ni bloquea la cuenta: deja de aparecer en listados y como autora.")
    @PatchMapping("/personas/{id}/ocultar")
    public PersonaAdminRespuesta ocultar(@PathVariable UUID id, @Valid @RequestBody OcultarPerfilDatos datos) {
        return personas.ocultar(id, datos.motivo(), usuarioActual.obtener().oid());
    }

    /** La devuelve a los listados. 409 si no estaba oculta por moderación. */
    @Operation(summary = "Muestra un perfil oculto por moderación")
    @PatchMapping("/personas/{id}/mostrar")
    public PersonaAdminRespuesta mostrar(@PathVariable UUID id) {
        return personas.mostrar(id, usuarioActual.obtener().oid());
    }

    /** Cantidad de perfiles en total, visibles y por rol, carrera y sede. */
    @Operation(summary = "Estadísticas de la comunidad")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cifras calculadas"),
            @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR", content = @Content)
    })
    @GetMapping("/estadisticas")
    public EstadisticasUsuariosRespuesta estadisticas() {
        return estadisticas.calcular();
    }
}
