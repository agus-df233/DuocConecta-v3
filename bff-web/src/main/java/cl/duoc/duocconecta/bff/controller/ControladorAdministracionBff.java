package cl.duoc.duocconecta.bff.controller;

import cl.duoc.duocconecta.bff.dto.AlertaActividadRespuesta;
import cl.duoc.duocconecta.bff.dto.ResumenAdministracionRespuesta;
import cl.duoc.duocconecta.bff.dto.FichaPersonaRespuesta;
import cl.duoc.duocconecta.bff.service.ClienteAnalitica;
import cl.duoc.duocconecta.bff.service.ClientePrompts;
import cl.duoc.duocconecta.bff.service.ClienteProyectos;
import cl.duoc.duocconecta.bff.service.ClienteUsuarios;
import java.util.ArrayList;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClientException;
import cl.duoc.duocconecta.bff.service.ResolvedorAutores;
import cl.duoc.duocconecta.bff.service.ServicioAdministracion;
import cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/** El dashboard de la vista de administración. Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/bff/admin")
@PreAuthorize(ReglasDeAcceso.SOLO_ADMINISTRADOR)
@Tag(name = "BFF · Administración", description = "Cifras agregadas para el dashboard de administración")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorAdministracionBff {

    private final ServicioAdministracion servicio;
    private final ClienteAnalitica analitica;
    private final ResolvedorAutores autores;
    private final ClienteUsuarios usuarios;
    private final ClienteProyectos proyectos;
    private final ClientePrompts prompts;

    public ControladorAdministracionBff(ServicioAdministracion servicio, ClienteAnalitica analitica,
                                        ResolvedorAutores autores, ClienteUsuarios usuarios,
                                        ClienteProyectos proyectos, ClientePrompts prompts) {
        this.servicio = servicio;
        this.analitica = analitica;
        this.autores = autores;
        this.usuarios = usuarios;
        this.proyectos = proyectos;
        this.prompts = prompts;
    }

    /**
     * La ficha de una persona: perfil, proyectos, prompts y alertas en una sola llamada. Si uno de
     * los servicios no responde, la ficha llega igual y dice qué parte falta.
     */
    @Operation(summary = "Ficha de una persona para la administración")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ficha armada (con noDisponible si faltó algo)"),
            @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR", content = @Content),
            @ApiResponse(responseCode = "404", description = "La persona no existe", content = @Content)
    })
    @GetMapping("/personas/{oid}")
    public FichaPersonaRespuesta ficha(@PathVariable String oid) {
        var faltan = new ArrayList<String>();
        var persona = usuarios.personaAdmin(oid);
        return new FichaPersonaRespuesta(persona,
                parte("proyectos", () -> proyectos.deDuena(oid), faltan),
                parte("prompts", () -> prompts.deAutor(oid), faltan),
                parte("alertas", () -> analitica.alertasDe(oid), faltan),
                faltan);
    }

    /** Una parte de la ficha; si su servicio falla, queda vacía y se anota. */
    private static <T> List<T> parte(String nombre, Supplier<List<T>> pedir, List<String> faltan) {
        try {
            return pedir.get();
        } catch (RestClientException error) {
            faltan.add(nombre);
            return List.of();
        }
    }

    /**
     * Cifras de la comunidad y de la vitrina en una sola llamada, con los proyectos contados por
     * la carrera de quien los publicó.
     */
    @Operation(summary = "Resumen para el dashboard de administración")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen armado"),
            @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR", content = @Content)
    })
    @GetMapping("/resumen")
    public ResumenAdministracionRespuesta resumen() {
        return servicio.resumen();
    }

    /**
     * Las alertas de actividad que detectó el consumidor de Kafka, con el nombre de cada persona.
     * ms-analitica solo conoce el {@code oid}; el nombre se pide una vez por persona.
     */
    @Operation(summary = "Alertas de actividad con la persona resuelta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertas"),
            @ApiResponse(responseCode = "403", description = "Sin rol ADMINISTRADOR", content = @Content),
            @ApiResponse(responseCode = "503", description = "ms-analitica no responde", content = @Content)
    })
    @GetMapping("/alertas")
    public List<AlertaActividadRespuesta> alertas(@RequestParam(defaultValue = "false") boolean pendientes) {
        var consulta = autores.abrir();
        return analitica.alertas(pendientes).stream()
                .map(a -> a.con(consulta.autorDe(a.usuarioId())))
                .toList();
    }
}
