package cl.duoc.duocconecta.prompts.service;

import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import cl.duoc.duocconecta.comun.eventos.TiposDeEvento;
import cl.duoc.duocconecta.comun.eventos.kafka.SenalDeUso;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import cl.duoc.duocconecta.prompts.domain.CategoriaPrompt;
import cl.duoc.duocconecta.prompts.domain.Prompt;
import cl.duoc.duocconecta.prompts.domain.Valoracion;
import cl.duoc.duocconecta.prompts.dto.PromptDatos;
import cl.duoc.duocconecta.prompts.dto.PromptRespuesta;
import cl.duoc.duocconecta.prompts.exception.OperacionNoPermitidaException;
import cl.duoc.duocconecta.prompts.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.prompts.exception.SolicitudInvalidaException;
import cl.duoc.duocconecta.prompts.repository.RepositorioPrompts;
import cl.duoc.duocconecta.prompts.repository.RepositorioValoraciones;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La biblioteca de prompts: publicar, buscar, bifurcar y valorar.
 *
 * <p>Un prompt que la persona no puede ver responde 404, igual que uno que no existe: distinguirlos
 * diría que hay un borrador ajeno con ese identificador. Los eventos salen después del commit
 * (SNS, RabbitMQ y Kafka) sin que este servicio sepa de brokers.</p>
 */
@Service
public class ServicioPrompts {

    /** Tope de un listado: la biblioteca se recorre con filtros, no entera. */
    private static final int MAXIMO = 200;

    private final RepositorioPrompts prompts;
    private final RepositorioValoraciones valoraciones;
    private final ApplicationEventPublisher eventos;

    public ServicioPrompts(RepositorioPrompts prompts, RepositorioValoraciones valoraciones,
                           ApplicationEventPublisher eventos) {
        this.prompts = prompts;
        this.valoraciones = valoraciones;
        this.eventos = eventos;
    }

    /** Orden de un listado. La tendencia la ordena el frontend con los datos de Kafka. */
    public enum Orden {
        RECIENTES,
        MEJOR_VALORADOS
    }

    /** Publica un prompt. */
    @Transactional
    public PromptRespuesta crear(PromptDatos datos, String autorId) {
        Prompt prompt = new Prompt();
        prompt.setAutorId(autorId);
        aplicar(prompt, datos);
        Prompt guardado = prompts.save(prompt);
        publicar(TiposDeEvento.PROMPT_PUBLICADO, autorId, null, guardado.getId(), null);
        return PromptRespuesta.desde(guardado, autorId, null);
    }

    /** Lo que la persona puede ver, con los filtros opcionales. */
    @Transactional(readOnly = true)
    public List<PromptRespuesta> buscar(String usuarioId, String texto, CategoriaPrompt categoria, String carrera,
                                        String herramienta, Orden orden) {
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";
        List<Prompt> encontrados = prompts.buscarVisibles(usuarioId, categoria, vacioANulo(carrera),
                vacioANulo(herramienta), patron, PageRequest.of(0, MAXIMO));
        if (orden == Orden.MEJOR_VALORADOS) {
            encontrados = encontrados.stream()
                    .sorted(Comparator.comparingDouble(Prompt::promedio).reversed()
                            .thenComparing(Comparator.comparingInt(Prompt::getCantidadValoraciones).reversed()))
                    .toList();
        }
        return encontrados.stream().map(p -> PromptRespuesta.desde(p, usuarioId, null)).toList();
    }

    /** Los prompts propios, incluidos los privados y los ocultos. */
    @Transactional(readOnly = true)
    public List<PromptRespuesta> mios(String usuarioId) {
        return prompts.findByAutorIdOrderByCreadoEnDesc(usuarioId).stream()
                .map(p -> PromptRespuesta.desde(p, usuarioId, null)).toList();
    }

    /** El detalle, con las estrellas que le dio quien mira. */
    @Transactional(readOnly = true)
    public PromptRespuesta obtener(UUID id, String usuarioId) {
        Prompt prompt = visible(id, usuarioId);
        Integer mia = valoraciones.findById(new Valoracion.Clave(id, usuarioId))
                .map(v -> (int) v.getEstrellas()).orElse(null);
        return PromptRespuesta.desde(prompt, usuarioId, mia);
    }

    /** Edita un prompt propio. */
    @Transactional
    public PromptRespuesta actualizar(UUID id, PromptDatos datos, String usuarioId) {
        Prompt prompt = propio(id, usuarioId);
        aplicar(prompt, datos);
        publicar(TiposDeEvento.PROMPT_ACTUALIZADO, usuarioId, null, id, null);
        return PromptRespuesta.desde(prompt, usuarioId, null);
    }

    /** Borra un prompt propio. Sus copias siguen, sin el enlace al original. */
    @Transactional
    public void eliminar(UUID id, String usuarioId) {
        Prompt prompt = propio(id, usuarioId);
        prompts.soltarCopias(id);
        prompts.delete(prompt);
        publicar(TiposDeEvento.PROMPT_ELIMINADO, usuarioId, null, id, null);
    }

    /**
     * Bifurca un prompt: crea una copia propia y editable que recuerda de dónde salió. La copia
     * nace privada, como un borrador: se publica cuando quien la hizo la ajusta.
     */
    @Transactional
    public PromptRespuesta bifurcar(UUID id, String usuarioId) {
        Prompt original = visible(id, usuarioId);
        if (original.getAutorId().equals(usuarioId)) {
            throw new SolicitudInvalidaException("Este prompt ya es tuyo: edítalo en vez de copiarlo");
        }
        Prompt copia = new Prompt();
        copia.setAutorId(usuarioId);
        copia.setTitulo(original.getTitulo());
        copia.setProposito(original.getProposito());
        copia.setContenido(original.getContenido());
        copia.setCategoria(original.getCategoria());
        copia.setCarrera(original.getCarrera());
        copia.setHerramienta(original.getHerramienta());
        copia.setVisibilidad(cl.duoc.duocconecta.prompts.domain.Visibilidad.PRIVADO);
        copia.setOrigen(original);
        Prompt guardada = prompts.save(copia);
        original.contarBifurcacion();
        publicar(TiposDeEvento.PROMPT_BIFURCADO, usuarioId, original.getAutorId(), id, guardada.getId().toString());
        return PromptRespuesta.desde(guardada, usuarioId, null);
    }

    /** Valora un prompt ajeno. Valorar de nuevo reemplaza las estrellas anteriores. */
    @Transactional
    public PromptRespuesta valorar(UUID id, int estrellas, String usuarioId) {
        Prompt prompt = visible(id, usuarioId);
        if (prompt.getAutorId().equals(usuarioId)) {
            throw new SolicitudInvalidaException("No puedes valorar tu propio prompt");
        }
        var clave = new Valoracion.Clave(id, usuarioId);
        var existente = valoraciones.findById(clave);
        prompt.aplicarValoracion(existente.map(v -> (int) v.getEstrellas()).orElse(null), estrellas);
        existente.ifPresentOrElse(
                v -> v.cambiar((short) estrellas),
                () -> valoraciones.save(new Valoracion(clave, (short) estrellas, Instant.now())));
        publicar(TiposDeEvento.PROMPT_VALORADO, usuarioId, prompt.getAutorId(), id, Integer.toString(estrellas));
        return PromptRespuesta.desde(prompt, usuarioId, estrellas);
    }

    /** Alguien abrió el detalle: señal para la tendencia, solo a Kafka. Las del autor no cuentan. */
    @Transactional(readOnly = true)
    public void registrarVista(UUID id, String usuarioId) {
        Prompt prompt = visible(id, usuarioId);
        if (!prompt.getAutorId().equals(usuarioId)) {
            eventos.publishEvent(SenalDeUso.vista(TipoRecurso.PROMPT, id.toString(), usuarioId));
        }
    }

    /** Alguien copió el texto: la señal de uso más fuerte, solo a Kafka. Las del autor no cuentan. */
    @Transactional(readOnly = true)
    public void registrarCopia(UUID id, String usuarioId) {
        Prompt prompt = visible(id, usuarioId);
        if (!prompt.getAutorId().equals(usuarioId)) {
            eventos.publishEvent(SenalDeUso.copia(id.toString(), usuarioId));
        }
    }

    // --- Ayudantes ---

    private Prompt visible(UUID id, String usuarioId) {
        return prompts.findById(id).filter(p -> p.esVisiblePara(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException("Prompt no encontrado: " + id));
    }

    private Prompt propio(UUID id, String usuarioId) {
        Prompt prompt = visible(id, usuarioId);
        if (!prompt.getAutorId().equals(usuarioId)) {
            throw new OperacionNoPermitidaException("Solo quien publicó el prompt puede cambiarlo");
        }
        return prompt;
    }

    private static void aplicar(Prompt prompt, PromptDatos datos) {
        prompt.setTitulo(datos.titulo().trim());
        prompt.setProposito(datos.proposito().trim());
        prompt.setContenido(datos.contenido());
        prompt.setCategoria(datos.categoria());
        prompt.setCarrera(vacioANulo(datos.carrera()));
        prompt.setHerramienta(vacioANulo(datos.herramienta()));
        prompt.setVisibilidad(datos.visibilidad());
    }

    private void publicar(String tipo, String actorId, String destinatarioId, UUID promptId, String detalle) {
        eventos.publishEvent(EventoDuocConecta.nuevo(tipo, actorId, destinatarioId, promptId.toString(), null, detalle));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
