package cl.duoc.duocconecta.bff.controller;

import cl.duoc.duocconecta.bff.dto.Autor;
import cl.duoc.duocconecta.bff.dto.PromptRespuesta;
import cl.duoc.duocconecta.bff.service.ClientePrompts;
import cl.duoc.duocconecta.bff.service.ResolvedorAutores;
import cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La biblioteca de prompts para el frontend: lo que responde ms-prompts, con el nombre de cada
 * autor. Publicar, editar, bifurcar y valorar van directo a ms-prompts: ahí no hay nada que componer.
 */
@RestController
@RequestMapping("/api/v1/bff/biblioteca")
@PreAuthorize(ReglasDeAcceso.ROLES_DE_LA_PLATAFORMA)
@Tag(name = "BFF · Biblioteca", description = "Prompts con su autor resuelto")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorBibliotecaBff {

    private final ClientePrompts prompts;
    private final ResolvedorAutores autores;

    public ControladorBibliotecaBff(ClientePrompts prompts, ResolvedorAutores autores) {
        this.prompts = prompts;
        this.autores = autores;
    }

    /** Busca prompts con los filtros de ms-prompts; cada autor se pide una sola vez. */
    @Operation(summary = "Busca en la biblioteca, con el autor de cada prompt")
    @GetMapping
    public List<PromptRespuesta> buscar(@RequestParam(required = false) String texto,
                                        @RequestParam(required = false) String categoria,
                                        @RequestParam(required = false) String carrera,
                                        @RequestParam(required = false) String herramienta,
                                        @RequestParam(required = false) String orden) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("texto", texto);
        filtros.put("categoria", categoria);
        filtros.put("carrera", carrera);
        filtros.put("herramienta", herramienta);
        filtros.put("orden", orden);
        var consulta = autores.abrir();
        return prompts.buscar(filtros).stream().map(p -> conAutores(p, consulta)).toList();
    }

    /** El detalle, con el autor y, si es una copia, el autor del original. */
    @Operation(summary = "Detalle de un prompt, con su autor y el del original")
    @GetMapping("/{id}")
    public PromptRespuesta obtener(@PathVariable UUID id) {
        return conAutores(prompts.obtener(id), autores.abrir());
    }

    private static PromptRespuesta conAutores(PromptRespuesta prompt, ResolvedorAutores.Consulta consulta) {
        Autor deOrigen = prompt.origen() == null ? null : consulta.autorDe(prompt.origen().autorId());
        return prompt.con(consulta.autorDe(prompt.autorId()), deOrigen);
    }
}
