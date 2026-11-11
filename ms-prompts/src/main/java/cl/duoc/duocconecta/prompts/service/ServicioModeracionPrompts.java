package cl.duoc.duocconecta.prompts.service;

import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import cl.duoc.duocconecta.comun.eventos.TiposDeEvento;
import cl.duoc.duocconecta.prompts.domain.Prompt;
import cl.duoc.duocconecta.prompts.domain.Visibilidad;
import cl.duoc.duocconecta.prompts.dto.Conteo;
import cl.duoc.duocconecta.prompts.dto.EstadisticasPromptsRespuesta;
import cl.duoc.duocconecta.prompts.dto.PromptRespuesta;
import cl.duoc.duocconecta.prompts.exception.ConflictoDeEstadoException;
import cl.duoc.duocconecta.prompts.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.prompts.repository.RepositorioPrompts;
import cl.duoc.duocconecta.prompts.repository.RepositorioValoraciones;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Moderación de la biblioteca, solo ADMINISTRADOR: ocultar con motivo, mostrar y borrar. */
@Service
public class ServicioModeracionPrompts {

    private final RepositorioPrompts prompts;
    private final RepositorioValoraciones valoraciones;
    private final ApplicationEventPublisher eventos;

    public ServicioModeracionPrompts(RepositorioPrompts prompts, RepositorioValoraciones valoraciones,
                                     ApplicationEventPublisher eventos) {
        this.prompts = prompts;
        this.valoraciones = valoraciones;
        this.eventos = eventos;
    }

    /** Todos los prompts, con filtro opcional por ocultos y por autor. */
    @Transactional(readOnly = true)
    public List<PromptRespuesta> listar(Boolean oculto, String autorId) {
        String autor = autorId == null || autorId.isBlank() ? null : autorId;
        return prompts.paraModerar(oculto, autor, PageRequest.of(0, 500)).stream().map(PromptRespuesta::paraModerar).toList();
    }

    /** Lo saca de la biblioteca sin borrarlo. Su autor sigue viéndolo, con el motivo. */
    @Transactional
    public PromptRespuesta ocultar(UUID id, String motivo, String administrador) {
        Prompt prompt = buscar(id);
        if (prompt.isOculto()) {
            throw new ConflictoDeEstadoException("El prompt ya está oculto");
        }
        prompt.ocultar(motivo.trim(), administrador);
        publicar(TiposDeEvento.PROMPT_OCULTADO, administrador, prompt, motivo.trim());
        return PromptRespuesta.paraModerar(prompt);
    }

    /** Lo devuelve a la biblioteca. */
    @Transactional
    public PromptRespuesta mostrar(UUID id, String administrador) {
        Prompt prompt = buscar(id);
        if (!prompt.isOculto()) {
            throw new ConflictoDeEstadoException("El prompt no está oculto");
        }
        prompt.mostrar();
        publicar(TiposDeEvento.PROMPT_RESTAURADO, administrador, prompt, null);
        return PromptRespuesta.paraModerar(prompt);
    }

    /** Lo borra. Sus copias siguen, sin enlace. */
    @Transactional
    public void eliminar(UUID id, String administrador) {
        Prompt prompt = buscar(id);
        prompts.soltarCopias(id);
        prompts.delete(prompt);
        publicar(TiposDeEvento.PROMPT_ELIMINADO, administrador, prompt, "moderacion");
    }

    /** Cifras para el dashboard. */
    @Transactional(readOnly = true)
    public EstadisticasPromptsRespuesta estadisticas() {
        return new EstadisticasPromptsRespuesta(prompts.count(),
                prompts.countByVisibilidadAndOcultoFalse(Visibilidad.PUBLICO),
                prompts.countByOcultoTrue(), valoraciones.total(),
                conteos(prompts.contarPorCategoria()), conteos(prompts.contarPorCarrera()));
    }

    private Prompt buscar(UUID id) {
        return prompts.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Prompt no encontrado: " + id));
    }

    private void publicar(String tipo, String administrador, Prompt prompt, String detalle) {
        eventos.publishEvent(EventoDuocConecta.nuevo(tipo, administrador, prompt.getAutorId(),
                prompt.getId().toString(), null, detalle));
    }

    private static List<Conteo> conteos(List<Object[]> filas) {
        return filas.stream().map(f -> new Conteo(String.valueOf(f[0]), ((Number) f[1]).longValue())).toList();
    }
}
