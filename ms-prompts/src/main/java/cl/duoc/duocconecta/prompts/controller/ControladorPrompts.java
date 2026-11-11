package cl.duoc.duocconecta.prompts.controller;

import static cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso.ROLES_DE_LA_PLATAFORMA;

import cl.duoc.duocconecta.comun.seguridad.UsuarioActual;
import cl.duoc.duocconecta.prompts.domain.CategoriaPrompt;
import cl.duoc.duocconecta.prompts.dto.PromptDatos;
import cl.duoc.duocconecta.prompts.dto.PromptRespuesta;
import cl.duoc.duocconecta.prompts.dto.ValoracionDatos;
import cl.duoc.duocconecta.prompts.service.ServicioPrompts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** La biblioteca de prompts (HU-12 a HU-15). El autor siempre sale del token. */
@RestController
@RequestMapping("/api/v1/prompts")
@PreAuthorize(ROLES_DE_LA_PLATAFORMA)
@Tag(name = "Prompts", description = "Publicar, buscar, bifurcar y valorar prompts")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorPrompts {

    private final ServicioPrompts servicio;
    private final UsuarioActual usuarioActual;

    public ControladorPrompts(ServicioPrompts servicio, UsuarioActual usuarioActual) {
        this.servicio = servicio;
        this.usuarioActual = usuarioActual;
    }

    private String yo() {
        return usuarioActual.obtener().oid();
    }

    /** Publica un prompt (HU-12). */
    @Operation(summary = "Publica un prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicado"),
            @ApiResponse(responseCode = "400", description = "Faltan datos o son demasiado largos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PromptRespuesta> crear(@Valid @RequestBody PromptDatos datos) {
        PromptRespuesta creado = servicio.crear(datos, yo());
        return ResponseEntity.created(URI.create("/api/v1/prompts/" + creado.id())).body(creado);
    }

    /** Busca en la biblioteca (HU-15). Todos los filtros son opcionales y se combinan. */
    @Operation(summary = "Busca prompts", description = "Los públicos de todos y los propios. Filtros opcionales y combinables.")
    @GetMapping
    public List<PromptRespuesta> buscar(
            @Parameter(description = "Texto en el título o el propósito") @RequestParam(required = false) String texto,
            @RequestParam(required = false) CategoriaPrompt categoria,
            @RequestParam(required = false) String carrera,
            @Parameter(description = "Herramienta de IA, por ejemplo ChatGPT") @RequestParam(required = false) String herramienta,
            @RequestParam(defaultValue = "RECIENTES") ServicioPrompts.Orden orden) {
        return servicio.buscar(yo(), texto, categoria, carrera, herramienta, orden);
    }

    /** Los prompts propios, también los privados y los ocultos por moderación. */
    @Operation(summary = "Mis prompts")
    @GetMapping("/mios")
    public List<PromptRespuesta> mios() {
        return servicio.mios(yo());
    }

    /** El detalle, con las estrellas que le diste. */
    @Operation(summary = "Detalle de un prompt", description = "404 si no existe o no lo puedes ver: un borrador ajeno no se distingue.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle"),
            @ApiResponse(responseCode = "404", description = "No existe o no lo puedes ver", content = @Content)
    })
    @GetMapping("/{id}")
    public PromptRespuesta obtener(@PathVariable UUID id) {
        return servicio.obtener(id, yo());
    }

    /** Edita un prompt propio. */
    @Operation(summary = "Edita un prompt propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizado"),
            @ApiResponse(responseCode = "403", description = "Es de otra persona", content = @Content)
    })
    @PutMapping("/{id}")
    public PromptRespuesta actualizar(@PathVariable UUID id, @Valid @RequestBody PromptDatos datos) {
        return servicio.actualizar(id, datos, yo());
    }

    /** Borra un prompt propio. Sus copias siguen. */
    @Operation(summary = "Borra un prompt propio", description = "Las copias que otras personas hicieron siguen, sin enlace al original.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicio.eliminar(id, yo());
        return ResponseEntity.noContent().build();
    }

    /** Bifurca: una copia propia y editable, que nace privada (HU-13). */
    @Operation(summary = "Bifurca un prompt", description = "Crea tu copia, privada, que recuerda de dónde salió. 400 si el prompt ya es tuyo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Copia creada"),
            @ApiResponse(responseCode = "400", description = "El prompt ya es tuyo", content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe o no lo puedes ver", content = @Content)
    })
    @PostMapping("/{id}/bifurcaciones")
    public ResponseEntity<PromptRespuesta> bifurcar(@PathVariable UUID id) {
        PromptRespuesta copia = servicio.bifurcar(id, yo());
        return ResponseEntity.created(URI.create("/api/v1/prompts/" + copia.id())).body(copia);
    }

    /** Valora con estrellas; valorar de nuevo reemplaza (HU-14). */
    @Operation(summary = "Valora un prompt", description = "De 1 a 5 estrellas, una vez por persona. 400 si es tuyo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valoración guardada"),
            @ApiResponse(responseCode = "400", description = "Fuera de rango, o el prompt es tuyo", content = @Content)
    })
    @PutMapping("/{id}/valoracion")
    public PromptRespuesta valorar(@PathVariable UUID id, @Valid @RequestBody ValoracionDatos datos) {
        return servicio.valorar(id, datos.estrellas(), yo());
    }

    /** Se abrió el detalle: señal para la tendencia (solo Kafka). */
    @Operation(summary = "Registra una vista", description = "Las del autor no cuentan. Solo llega a Kafka.")
    @PostMapping("/{id}/vistas")
    public ResponseEntity<Void> registrarVista(@PathVariable UUID id) {
        servicio.registrarVista(id, yo());
        return ResponseEntity.noContent().build();
    }

    /** Se copió el texto: la señal de uso más fuerte (solo Kafka). */
    @Operation(summary = "Registra una copia del texto", description = "Las del autor no cuentan. Solo llega a Kafka.")
    @PostMapping("/{id}/copias")
    public ResponseEntity<Void> registrarCopia(@PathVariable UUID id) {
        servicio.registrarCopia(id, yo());
        return ResponseEntity.noContent().build();
    }
}
