package cl.duoc.duocconecta.prompts.controller;

import cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso;
import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import cl.duoc.duocconecta.prompts.dto.EstadisticasPromptsRespuesta;
import cl.duoc.duocconecta.prompts.dto.OcultarPromptDatos;
import cl.duoc.duocconecta.prompts.dto.PromptRespuesta;
import cl.duoc.duocconecta.prompts.service.ServicioModeracionPrompts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderación de la biblioteca, solo ADMINISTRADOR. Vive bajo {@code /api/v1/prompts/admin} para que
 * la misma ruta del ALB y del API Gateway lo cubra.
 */
@RestController
@RequestMapping("/api/v1/prompts/admin")
@PreAuthorize(ReglasDeAcceso.SOLO_ADMINISTRADOR)
@Tag(name = "Moderación de prompts", description = "Ocultar, mostrar y borrar prompts. Solo ADMINISTRADOR")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorModeracionPrompts {

    private final ServicioModeracionPrompts servicio;
    private final UsuarioActual usuarioActual;

    public ControladorModeracionPrompts(ServicioModeracionPrompts servicio, UsuarioActual usuarioActual) {
        this.servicio = servicio;
        this.usuarioActual = usuarioActual;
    }

    /** Todos los prompts, ocultos incluidos. */
    @Operation(summary = "Lista los prompts para moderar")
    @GetMapping
    public List<PromptRespuesta> listar(@RequestParam(required = false) Boolean oculto) {
        return servicio.listar(oculto);
    }

    /** Cifras de la biblioteca para el dashboard. */
    @Operation(summary = "Estadísticas de la biblioteca")
    @GetMapping("/estadisticas")
    public EstadisticasPromptsRespuesta estadisticas() {
        return servicio.estadisticas();
    }

    /** Lo saca de la biblioteca con un motivo. 409 si ya está oculto. */
    @Operation(summary = "Oculta un prompt")
    @PatchMapping("/{id}/ocultar")
    public PromptRespuesta ocultar(@PathVariable UUID id, @Valid @RequestBody OcultarPromptDatos datos) {
        return servicio.ocultar(id, datos.motivo(), usuarioActual.obtener().oid());
    }

    /** Lo devuelve a la biblioteca. 409 si no estaba oculto. */
    @Operation(summary = "Muestra un prompt oculto")
    @PatchMapping("/{id}/mostrar")
    public PromptRespuesta mostrar(@PathVariable UUID id) {
        return servicio.mostrar(id, usuarioActual.obtener().oid());
    }

    /** Lo borra. */
    @Operation(summary = "Elimina un prompt")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicio.eliminar(id, usuarioActual.obtener().oid());
        return ResponseEntity.noContent().build();
    }
}
