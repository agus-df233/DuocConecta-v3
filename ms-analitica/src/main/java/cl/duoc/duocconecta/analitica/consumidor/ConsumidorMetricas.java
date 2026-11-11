package cl.duoc.duocconecta.analitica.consumidor;

import cl.duoc.duocconecta.analitica.service.ServicioMetricas;
import cl.duoc.duocconecta.comun.eventos.kafka.ActividadUsuario;
import cl.duoc.duocconecta.comun.eventos.kafka.InteraccionContenido;
import cl.duoc.duocconecta.comun.eventos.kafka.RegistroKafka;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Grupo {@code analitica-metricas}: lee <b>los dos</b> tópicos y cuenta los registros por hora.
 *
 * <p>Es la prueba de que dos grupos leen lo mismo sin quitarse nada: los registros que procesan
 * {@code analitica-tendencias} y {@code analitica-alertas} llegan también aquí, y cada grupo lleva
 * sus propios offsets.</p>
 */
@Component
public class ConsumidorMetricas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorMetricas.class);

    private final ServicioMetricas servicio;
    private final String grupo;

    public ConsumidorMetricas(ServicioMetricas servicio, @Value("${duocconecta.analitica.grupos.metricas}") String grupo) {
        this.servicio = servicio;
        this.grupo = grupo;
    }

    /** Las interacciones, contadas por hora. */
    @KafkaListener(id = "metricas-interacciones",
            groupId = "${duocconecta.analitica.grupos.metricas}",
            clientIdPrefix = "${duocconecta.analitica.grupos.metricas}-interacciones",
            topics = "${duocconecta.kafka.topicos.interacciones.nombre}",
            properties = "spring.json.value.default.type=cl.duoc.duocconecta.comun.eventos.kafka.InteraccionContenido")
    public void interaccion(@Payload @Valid InteraccionContenido interaccion,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        // El consumidor de idempotencia lleva el tópico: el mismo idEvento llega a los dos.
        boolean nuevo = servicio.registrar(interaccion.idEvento(), grupo + "-" + RegistroKafka.INTERACCIONES,
                RegistroKafka.INTERACCIONES, interaccion.tipo().name(), interaccion.ocurridoEn());
        log.debug("{}: {} contado (partición {}, offset {}, nuevo {})", grupo, interaccion.tipo(), particion, offset, nuevo);
    }

    /** La actividad, contada por hora. */
    @KafkaListener(id = "metricas-actividad",
            groupId = "${duocconecta.analitica.grupos.metricas}",
            clientIdPrefix = "${duocconecta.analitica.grupos.metricas}-actividad",
            topics = "${duocconecta.kafka.topicos.actividad.nombre}",
            properties = "spring.json.value.default.type=cl.duoc.duocconecta.comun.eventos.kafka.ActividadUsuario")
    public void actividad(@Payload @Valid ActividadUsuario accion,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        boolean nuevo = servicio.registrar(accion.idEvento(), grupo + "-" + RegistroKafka.ACTIVIDAD,
                RegistroKafka.ACTIVIDAD, accion.tipo().name(), accion.ocurridoEn());
        log.debug("{}: {} contado (partición {}, offset {}, nuevo {})", grupo, accion.tipo(), particion, offset, nuevo);
    }
}
