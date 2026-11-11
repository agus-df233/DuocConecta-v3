package cl.duoc.duocconecta.analitica.consumidor;

import cl.duoc.duocconecta.analitica.service.ServicioTendencias;
import cl.duoc.duocconecta.comun.eventos.kafka.InteraccionContenido;
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
 * Grupo {@code analitica-tendencias}: lee las interacciones y actualiza la tendencia.
 *
 * <p>Solo adapta: el DTO llega validado ({@code @Valid}), se revisa la clave y el resto lo hace
 * {@link ServicioTendencias}. Un error va al manejador del contenedor: reintenta los recuperables
 * y manda a la DLT lo que no tiene arreglo.</p>
 */
@Component
public class ConsumidorTendencias {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorTendencias.class);

    private final ServicioTendencias servicio;
    private final String grupo;

    public ConsumidorTendencias(ServicioTendencias servicio,
                                @Value("${duocconecta.analitica.grupos.tendencias}") String grupo) {
        this.servicio = servicio;
        this.grupo = grupo;
    }

    /** Un registro de {@code duocconecta.contenido.interacciones}. */
    @KafkaListener(id = "tendencias",
            groupId = "${duocconecta.analitica.grupos.tendencias}",
            clientIdPrefix = "${duocconecta.analitica.grupos.tendencias}",
            topics = "${duocconecta.kafka.topicos.interacciones.nombre}",
            properties = "spring.json.value.default.type=cl.duoc.duocconecta.comun.eventos.kafka.InteraccionContenido")
    public void recibir(@Payload @Valid InteraccionContenido interaccion,
                        @Header(KafkaHeaders.RECEIVED_KEY) String clave,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        ReglasDeRegistro.exigirClave(clave, interaccion.recursoId(), "recursoId");
        boolean nuevo = servicio.registrar(interaccion, grupo);
        log.info("{}: {} de {} {} (partición {}, offset {}){}", grupo, interaccion.tipo(), interaccion.tipoRecurso(),
                interaccion.recursoId(), particion, offset, nuevo ? "" : " — repetido, se confirma sin volver a contarlo");
    }
}
