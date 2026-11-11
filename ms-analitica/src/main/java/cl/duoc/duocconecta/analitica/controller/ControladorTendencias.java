package cl.duoc.duocconecta.analitica.controller;

import static cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso.ROLES_DE_LA_PLATAFORMA;

import cl.duoc.duocconecta.analitica.dto.TendenciaRespuesta;
import cl.duoc.duocconecta.analitica.service.ServicioTendencias;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La tendencia que arman los consumidores de Kafka. Devuelve identificadores y números: el
 * frontend los cruza con lo que ya puede ver.
 */
@RestController
@RequestMapping("/api/v1/analitica")
@Validated
@Tag(name = "Tendencias", description = "Lo que más interés tuvo en los últimos 7 días")
public class ControladorTendencias {

    private final ServicioTendencias servicio;

    public ControladorTendencias(ServicioTendencias servicio) {
        this.servicio = servicio;
    }

    /** Los proyectos o prompts con más puntaje en la ventana. */
    @Operation(summary = "Lista lo que está en tendencia",
            description = "Ordenado por puntaje: cada interacción suma según su peso. Los ocultos y eliminados no salen.")
    @GetMapping("/tendencias")
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public List<TendenciaRespuesta> tendencias(
            @Parameter(description = "PROYECTO o PROMPT") @RequestParam(defaultValue = "PROYECTO") TipoRecurso tipoRecurso,
            @Parameter(description = "Cuántos, de 1 a 50") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite) {
        return servicio.tendencias(tipoRecurso, limite);
    }

    /** Los conteos de un recurso en la ventana: por ejemplo, "12 vistas esta semana". */
    @Operation(summary = "Interacciones de un recurso en los últimos 7 días")
    @GetMapping("/recursos/{recursoId}")
    @PreAuthorize(ROLES_DE_LA_PLATAFORMA)
    public TendenciaRespuesta deRecurso(@Parameter(description = "Identificador del proyecto o prompt")
                                        @PathVariable String recursoId) {
        return servicio.deRecurso(recursoId);
    }
}
