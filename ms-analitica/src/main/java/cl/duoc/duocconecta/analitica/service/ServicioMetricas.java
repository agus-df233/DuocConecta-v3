package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.domain.MetricaHora;
import cl.duoc.duocconecta.analitica.dto.MetricaHoraRespuesta;
import cl.duoc.duocconecta.analitica.repository.RepositorioMetricas;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuenta los registros por hora, por tópico y por tipo. Lee los dos tópicos con su propio grupo:
 * los mismos registros que procesan la tendencia y el detector, cada grupo con sus offsets.
 */
@Service
public class ServicioMetricas {

    private final RepositorioMetricas metricas;
    private final RegistroDeProcesados procesados;

    public ServicioMetricas(RepositorioMetricas metricas, RegistroDeProcesados procesados) {
        this.metricas = metricas;
        this.procesados = procesados;
    }

    /** Suma un registro a su hora. Devuelve falso si era un duplicado. */
    @Transactional
    public boolean registrar(String idEvento, String consumidor, String topico, String tipo, Instant ocurridoEn) {
        if (!procesados.primeraVez(idEvento, consumidor)) {
            return false;
        }
        var clave = new MetricaHora.Clave(ocurridoEn.truncatedTo(ChronoUnit.HOURS), topico, tipo);
        MetricaHora metrica = metricas.findById(clave).orElseGet(() -> new MetricaHora(clave));
        metrica.sumar();
        metricas.save(metrica);
        return true;
    }

    /** Las últimas horas, en orden. */
    @Transactional(readOnly = true)
    public List<MetricaHoraRespuesta> serie(int horas) {
        Instant desde = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(horas - 1L, ChronoUnit.HOURS);
        return metricas.desde(desde).stream()
                .map(m -> new MetricaHoraRespuesta(m.getClave().getHora(), m.getClave().getTopico(),
                        m.getClave().getTipo(), m.getCantidad()))
                .toList();
    }
}
