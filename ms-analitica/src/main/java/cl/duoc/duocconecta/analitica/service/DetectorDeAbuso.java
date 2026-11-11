package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.config.PropiedadesAnalitica;
import cl.duoc.duocconecta.analitica.domain.ActividadReciente;
import cl.duoc.duocconecta.analitica.domain.Alerta;
import cl.duoc.duocconecta.analitica.repository.RepositorioActividad;
import cl.duoc.duocconecta.analitica.repository.RepositorioAlertas;
import cl.duoc.duocconecta.comun.eventos.kafka.ActividadUsuario;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detecta ráfagas: alguien comenta, pide colaboración o valora muchas veces en pocos minutos.
 *
 * <p>Cuenta con la hora del hecho ({@code ocurridoEn}), no con la de llegada: si el consumidor
 * estuvo detenido y se pone al día de golpe, no inventa una ráfaga que no ocurrió. La clave del
 * tópico es la persona, así sus acciones llegan en orden a un solo hilo.</p>
 */
@Service
public class DetectorDeAbuso {

    private static final Logger log = LoggerFactory.getLogger(DetectorDeAbuso.class);

    private final RepositorioActividad actividad;
    private final RepositorioAlertas alertas;
    private final RegistroDeProcesados procesados;
    private final PropiedadesAnalitica propiedades;

    public DetectorDeAbuso(RepositorioActividad actividad, RepositorioAlertas alertas,
                           RegistroDeProcesados procesados, PropiedadesAnalitica propiedades) {
        this.actividad = actividad;
        this.alertas = alertas;
        this.procesados = procesados;
        this.propiedades = propiedades;
    }

    /**
     * Anota la acción y, si con ella se pasa el umbral, crea la alerta (o extiende la de la misma
     * ráfaga). Devuelve falso si era un duplicado.
     */
    @Transactional
    public boolean evaluar(ActividadUsuario accion, String consumidor) {
        if (!procesados.primeraVez(accion.idEvento(), consumidor)) {
            return false;
        }
        actividad.save(new ActividadReciente(UUID.randomUUID(), accion.usuarioId(), accion.tipo(),
                accion.recursoId(), accion.ocurridoEn()));

        PropiedadesAnalitica.Regla regla = propiedades.getAlertas().get(accion.tipo());
        if (regla == null) {
            return true;
        }
        Instant desde = accion.ocurridoEn().minus(Duration.ofMinutes(regla.getVentanaMinutos()));
        int cantidad = (int) actividad.countByUsuarioIdAndTipoAndOcurridoEnBetween(
                accion.usuarioId(), accion.tipo(), desde, accion.ocurridoEn());
        if (cantidad < regla.getUmbral()) {
            return true;
        }
        alertas.findFirstByUsuarioIdAndTipoAndHastaGreaterThanEqualOrderByHastaDesc(accion.usuarioId(), accion.tipo(), desde)
                .ifPresentOrElse(
                        abierta -> abierta.extender(cantidad, accion.recursoId(), accion.ocurridoEn()),
                        () -> {
                            alertas.save(new Alerta(accion.usuarioId(), accion.tipo(), cantidad,
                                    regla.getVentanaMinutos(), accion.recursoId(), desde, accion.ocurridoEn()));
                            log.warn("Alerta: {} hizo {} {} en {} min (umbral {})", accion.usuarioId(), cantidad,
                                    accion.tipo(), regla.getVentanaMinutos(), regla.getUmbral());
                        });
        return true;
    }
}
