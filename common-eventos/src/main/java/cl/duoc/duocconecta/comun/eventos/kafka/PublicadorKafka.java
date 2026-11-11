package cl.duoc.duocconecta.comun.eventos.kafka;

import cl.duoc.duocconecta.comun.eventos.AtributosMensaje;
import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Envía a Kafka los registros de la analítica.
 *
 * <p>El envío es asíncrono: el resultado (partición y offset, o el error) queda en el log cuando
 * el broker responde. Si Kafka no está, se registra un ERROR y el negocio sigue: la analítica se
 * pierde, el cambio que hizo la persona no.</p>
 */
public class PublicadorKafka {

    private static final Logger log = LoggerFactory.getLogger(PublicadorKafka.class);

    private final KafkaTemplate<String, Object> plantilla;
    private final PropiedadesKafka propiedades;
    private final TraductorEventosKafka traductor;

    public PublicadorKafka(KafkaTemplate<String, Object> plantilla, PropiedadesKafka propiedades,
                           TraductorEventosKafka traductor) {
        this.plantilla = plantilla;
        this.propiedades = propiedades;
        this.traductor = traductor;
    }

    /** Publica lo que le corresponde a Kafka de un evento de negocio ya confirmado. */
    public void publicar(EventoDuocConecta evento) {
        traductor.traducir(evento).forEach(this::enviar);
    }

    /**
     * Publica una señal de uso: alguien abrió o copió un contenido. No es un evento de negocio
     * (nadie recibe un aviso por eso), así que va solo a Kafka.
     */
    public void publicarUso(TipoInteraccion tipo, TipoActividad actividad, TipoRecurso tipoRecurso,
                            String recursoId, String usuarioId) {
        traductor.uso(tipo, actividad, tipoRecurso, recursoId, usuarioId, UUID.randomUUID().toString(),
                Instant.now()).forEach(this::enviar);
    }

    private void enviar(RegistroKafka registro) {
        String topico = propiedades.topico(registro.claveTopico());
        if (!propiedades.isHabilitada()) {
            log.debug("Kafka deshabilitada: el registro {} de {} no se envía", registro.tipo(), topico);
            return;
        }
        ProducerRecord<String, Object> mensaje = new ProducerRecord<>(topico, registro.clave(), registro.valor());
        cabecera(mensaje, CabecerasKafka.TIPO, registro.tipo());
        cabecera(mensaje, CabecerasKafka.ORIGEN, propiedades.getOrigen());
        cabecera(mensaje, CabecerasKafka.ID_PETICION, MDC.get(AtributosMensaje.CLAVE_MDC));
        try {
            plantilla.send(mensaje).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Kafka no guardó {} en {} (clave {}): {}", registro.tipo(), topico,
                            registro.clave(), error.getMessage());
                } else {
                    var metadatos = resultado.getRecordMetadata();
                    log.info("{} publicado en {} (clave {}, partición {}, offset {})", registro.tipo(), topico,
                            registro.clave(), metadatos.partition(), metadatos.offset());
                }
            });
        } catch (RuntimeException error) {
            // Sin brokers, send() espera la metadata hasta max.block.ms y falla aquí, en este hilo.
            log.error("No se pudo enviar {} a {}: {}", registro.tipo(), topico, error.getMessage());
        }
    }

    private static void cabecera(ProducerRecord<String, Object> mensaje, String nombre, String valor) {
        if (valor != null && !valor.isBlank()) {
            mensaje.headers().add(nombre, valor.getBytes(StandardCharsets.UTF_8));
        }
    }
}
