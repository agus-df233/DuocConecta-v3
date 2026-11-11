package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.config.PropiedadesAnalitica;
import cl.duoc.duocconecta.analitica.domain.InteraccionDiaria;
import cl.duoc.duocconecta.analitica.domain.RecursoExcluido;
import cl.duoc.duocconecta.analitica.dto.TendenciaRespuesta;
import cl.duoc.duocconecta.analitica.repository.RepositorioExcluidos;
import cl.duoc.duocconecta.analitica.repository.RepositorioInteracciones;
import cl.duoc.duocconecta.comun.eventos.kafka.InteraccionContenido;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La tendencia de proyectos y prompts: qué tuvo más interés en los últimos 7 días.
 *
 * <p>Cada interacción suma un peso (una solicitud de colaboración dice más que una vista). Ocultar
 * o eliminar un recurso lo saca de la lista; restaurarlo lo devuelve. Como todos los registros de
 * un recurso caen en la misma partición, llegan en el orden en que pasaron.</p>
 */
@Service
public class ServicioTendencias {

    private static final Logger log = LoggerFactory.getLogger(ServicioTendencias.class);

    private final RepositorioInteracciones interacciones;
    private final RepositorioExcluidos excluidos;
    private final RegistroDeProcesados procesados;
    private final PropiedadesAnalitica propiedades;

    public ServicioTendencias(RepositorioInteracciones interacciones, RepositorioExcluidos excluidos,
                              RegistroDeProcesados procesados, PropiedadesAnalitica propiedades) {
        this.interacciones = interacciones;
        this.excluidos = excluidos;
        this.procesados = procesados;
        this.propiedades = propiedades;
    }

    /** Aplica una interacción a la vista. Devuelve falso si era un duplicado. */
    @Transactional
    public boolean registrar(InteraccionContenido interaccion, String consumidor) {
        if (!procesados.primeraVez(interaccion.idEvento(), consumidor)) {
            return false;
        }
        switch (interaccion.tipo()) {
            case OCULTADO, ELIMINADO -> excluidos.save(new RecursoExcluido(interaccion.recursoId(),
                    interaccion.tipoRecurso(), interaccion.tipo(), interaccion.ocurridoEn()));
            case RESTAURADO -> excluidos.findById(interaccion.recursoId())
                    .filter(e -> e.getMotivo() == TipoInteraccion.OCULTADO)
                    .ifPresent(excluidos::delete);
            case PUBLICADO -> {
                // Nace sin puntaje: aparecerá cuando alguien interactúe con él.
            }
            default -> sumar(interaccion);
        }
        return true;
    }

    private void sumar(InteraccionContenido interaccion) {
        LocalDate dia = interaccion.ocurridoEn().atZone(ZoneOffset.UTC).toLocalDate();
        var clave = new InteraccionDiaria.Clave(interaccion.tipoRecurso(), interaccion.recursoId(), dia,
                interaccion.tipo());
        InteraccionDiaria conteo = interacciones.findById(clave).orElseGet(() -> new InteraccionDiaria(clave));
        conteo.sumar();
        interacciones.save(conteo);
        log.debug("{} {} suma una {} ({} hoy)", interaccion.tipoRecurso(), interaccion.recursoId(),
                interaccion.tipo(), conteo.getCantidad());
    }

    /** Los recursos con más puntaje en la ventana, de mayor a menor. */
    @Transactional(readOnly = true)
    public List<TendenciaRespuesta> tendencias(TipoRecurso tipoRecurso, int limite) {
        Map<String, Map<TipoInteraccion, Long>> porRecurso = interacciones.vigentes(tipoRecurso, desde()).stream()
                .collect(Collectors.groupingBy(i -> i.getClave().getRecursoId(), LinkedHashMap::new,
                        Collectors.groupingBy(i -> i.getClave().getTipo(), () -> new EnumMap<>(TipoInteraccion.class),
                                Collectors.summingLong(InteraccionDiaria::getCantidad))));
        return porRecurso.entrySet().stream()
                .map(e -> new TendenciaRespuesta(tipoRecurso, e.getKey(), puntaje(tipoRecurso, e.getValue()), e.getValue()))
                .filter(t -> t.puntaje() > 0)
                .sorted(Comparator.comparingLong(TendenciaRespuesta::puntaje).reversed()
                        .thenComparing(TendenciaRespuesta::recursoId))
                .limit(limite)
                .toList();
    }

    /** Los conteos de un recurso en la ventana, aunque no esté entre los primeros. */
    @Transactional(readOnly = true)
    public TendenciaRespuesta deRecurso(String recursoId) {
        List<InteraccionDiaria> filas = interacciones.deRecurso(recursoId, desde());
        TipoRecurso tipoRecurso = filas.isEmpty() ? null : filas.get(0).getClave().getTipoRecurso();
        Map<TipoInteraccion, Long> conteos = filas.stream().collect(Collectors.groupingBy(
                i -> i.getClave().getTipo(), () -> new EnumMap<>(TipoInteraccion.class),
                Collectors.summingLong(InteraccionDiaria::getCantidad)));
        long puntaje = tipoRecurso == null || excluidos.existsById(recursoId) ? 0 : puntaje(tipoRecurso, conteos);
        return new TendenciaRespuesta(tipoRecurso, recursoId, puntaje, conteos);
    }

    private long puntaje(TipoRecurso tipoRecurso, Map<TipoInteraccion, Long> conteos) {
        return conteos.entrySet().stream()
                .mapToLong(e -> e.getValue() * propiedades.peso(tipoRecurso, e.getKey()))
                .sum();
    }

    private LocalDate desde() {
        return Instant.now().atZone(ZoneOffset.UTC).toLocalDate().minusDays(propiedades.getVentanaTendenciaDias() - 1L);
    }
}
