package cl.duoc.duocconecta.bff.controller;

import cl.duoc.duocconecta.bff.dto.AlertaActividadRespuesta;
import cl.duoc.duocconecta.bff.dto.ResumenAdministracionRespuesta;
import cl.duoc.duocconecta.bff.service.ClienteAnalitica;
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

    public ControladorAdministracionBff(ServicioAdministracion servicio, ClienteAnalitica analitica,
                                        ResolvedorAutores autores) {
        this.servicio = servicio;
        this.analitica = analitica;
        this.autores = autores;
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
