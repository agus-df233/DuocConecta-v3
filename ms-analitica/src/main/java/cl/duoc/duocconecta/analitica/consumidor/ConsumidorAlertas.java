package cl.duoc.duocconecta.analitica.consumidor;

import cl.duoc.duocconecta.analitica.service.DetectorDeAbuso;
import cl.duoc.duocconecta.comun.eventos.kafka.ActividadUsuario;
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
 * Grupo {@code analitica-alertas}: lee la actividad de las personas y detecta ráfagas. La clave es
 * el {@code oid}, así todo lo de una persona llega en orden a un solo hilo de este grupo.
 */
@Component
public class ConsumidorAlertas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorAlertas.class);

    private final DetectorDeAbuso detector;
    private final String grupo;

    public ConsumidorAlertas(DetectorDeAbuso detector, @Value("${duocconecta.analitica.grupos.alertas}") String grupo) {
        this.detector = detector;
        this.grupo = grupo;
    }

    /** Un registro de {@code duocconecta.usuarios.actividad}. */
    @KafkaListener(id = "alertas",
            groupId = "${duocconecta.analitica.grupos.alertas}",
            clientIdPrefix = "${duocconecta.analitica.grupos.alertas}",
            topics = "${duocconecta.kafka.topicos.actividad.nombre}",
            properties = "spring.json.value.default.type=cl.duoc.duocconecta.comun.eventos.kafka.ActividadUsuario")
    public void recibir(@Payload @Valid ActividadUsuario accion,
                        @Header(KafkaHeaders.RECEIVED_KEY) String clave,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        ReglasDeRegistro.exigirClave(clave, accion.usuarioId(), "usuarioId");
        boolean nuevo = detector.evaluar(accion, grupo);
        log.info("{}: {} de {} (partición {}, offset {}){}", grupo, accion.tipo(), accion.usuarioId(), particion,
                offset, nuevo ? "" : " — repetido, se confirma sin volver a contarlo");
    }
}
