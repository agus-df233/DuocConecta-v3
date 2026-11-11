package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.config.PropiedadesAnalitica;
import cl.duoc.duocconecta.analitica.repository.RepositorioActividad;
import cl.duoc.duocconecta.analitica.repository.RepositorioEventosProcesados;
import cl.duoc.duocconecta.analitica.repository.RepositorioInteracciones;
import cl.duoc.duocconecta.analitica.repository.RepositorioMetricas;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borra lo que ya no sirve, con los mismos plazos que la retención de los tópicos: la actividad de
 * una persona no vive aquí más que en Kafka.
 */
@Component
public class LimpiezaAnalitica {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaAnalitica.class);

    private final RepositorioActividad actividad;
    private final RepositorioInteracciones interacciones;
    private final RepositorioMetricas metricas;
    private final RepositorioEventosProcesados procesados;
    private final PropiedadesAnalitica propiedades;

    public LimpiezaAnalitica(RepositorioActividad actividad, RepositorioInteracciones interacciones,
                             RepositorioMetricas metricas, RepositorioEventosProcesados procesados,
                             PropiedadesAnalitica propiedades) {
        this.actividad = actividad;
        this.interacciones = interacciones;
        this.metricas = metricas;
        this.procesados = procesados;
        this.propiedades = propiedades;
    }

    /** Cada hora. */
    @Scheduled(cron = "${duocconecta.analitica.limpieza-cron:0 7 * * * *}")
    @Transactional
    public void limpiar() {
        Instant ahora = Instant.now();
        int dias = propiedades.getVentanaTendenciaDias();
        int borradas = actividad.borrarAnteriores(ahora.minus(Duration.ofHours(propiedades.getRetencionActividadHoras())));
        interacciones.borrarAnteriores(LocalDate.now(ZoneOffset.UTC).minusDays(dias));
        metricas.borrarAnteriores(ahora.minus(Duration.ofDays(dias)));
        // Un día de margen sobre la retención: antes de eso, un registro todavía podría releerse.
        procesados.borrarAnteriores(ahora.minus(Duration.ofDays(dias + 1L)));
        if (borradas > 0) {
            log.info("Limpieza: {} acciones de más de {} h borradas", borradas, propiedades.getRetencionActividadHoras());
        }
    }
}
