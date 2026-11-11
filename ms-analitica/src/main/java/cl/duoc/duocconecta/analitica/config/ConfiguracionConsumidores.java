package cl.duoc.duocconecta.analitica.config;

import cl.duoc.duocconecta.analitica.exception.EventoNoProcesableException;
import cl.duoc.duocconecta.comun.eventos.AtributosMensaje;
import cl.duoc.duocconecta.comun.eventos.kafka.CabecerasKafka;
import cl.duoc.duocconecta.comun.eventos.kafka.PropiedadesKafka;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.validation.Validator;

/**
 * Cómo reaccionan los consumidores ante un error. La conexión, la deserialización y los grupos
 * están en {@code spring.kafka.*} del {@code application.yml}; aquí va lo que necesita código.
 *
 * <ul>
 *   <li><b>Validación:</b> el {@code @Valid} de cada listener valida el DTO antes de procesarlo.</li>
 *   <li><b>Reintentos:</b> un error recuperable (la base no respondió) se reintenta 3 veces, a
 *       1, 2 y 4 segundos. Mientras tanto la partición espera: el orden no se rompe.</li>
 *   <li><b>DLT:</b> si se agotan los reintentos, o el registro no tiene arreglo (JSON inválido,
 *       DTO inválido, versión desconocida, clave cambiada), se copia a {@code <tópico>.dlt} en la
 *       misma partición, con el motivo en una cabecera, y se sigue con el próximo.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({PropiedadesAnalitica.class, PropiedadesKafka.class})
public class ConfiguracionConsumidores {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionConsumidores.class);

    /** Cabecera con el motivo del descarte, igual que el atributo de las DLQ de SQS. */
    static final String MOTIVO = AtributosMensaje.MOTIVO_DESCARTE;

    /** El {@code @Valid} de los listeners usa el mismo validador que los controladores. */
    @Bean
    public KafkaListenerConfigurer validacionDeRegistros(@Qualifier("defaultValidator") Validator validador) {
        return registrar -> registrar.setValidator(validador);
    }

    /**
     * Productor de las DLT. Un registro que no se pudo leer llega como bytes (el JSON roto tal
     * cual), y uno que se leyó pero no se pudo procesar llega como DTO: cada uno con su serializador.
     */
    @Bean
    public KafkaTemplate<Object, Object> plantillaDescartes(KafkaProperties propiedadesKafka) {
        Map<String, Object> configuracion = propiedadesKafka.buildProducerProperties();
        configuracion.put("acks", "all");
        configuracion.put("enable.idempotence", true);
        configuracion.put("max.block.ms", 5000);
        Map<Class<?>, Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        JacksonJsonSerializer<Object> json = new JacksonJsonSerializer<>();
        json.setAddTypeInfo(false);
        porTipo.put(Object.class, json);
        var fabrica = new DefaultKafkaProducerFactory<Object, Object>(configuracion,
                null, new DelegatingByTypeSerializer(porTipo, true));
        fabrica.setKeySerializer(castear(new StringSerializer()));
        return new KafkaTemplate<>(fabrica);
    }

    /** Reintentos con espera creciente y, al final, la DLT. Spring Boot lo conecta a todos los listeners. */
    @Bean
    public CommonErrorHandler manejadorDeErrores(KafkaTemplate<Object, Object> plantillaDescartes,
                                                 PropiedadesKafka propiedades,
                                                 @Value("${duocconecta.analitica.grupos.metricas}") String grupoMetricas) {
        var recuperador = new DeadLetterPublishingRecoverer(plantillaDescartes,
                (registro, error) -> new TopicPartition(registro.topic() + propiedades.getSufijoDlt(), registro.partition()));
        recuperador.setHeadersFunction((registro, error) -> {
            RecordHeaders cabeceras = new RecordHeaders();
            cabeceras.add(new RecordHeader(MOTIVO, motivo(error).getBytes(StandardCharsets.UTF_8)));
            return cabeceras;
        });

        ExponentialBackOff espera = new ExponentialBackOff(1000, 2);
        espera.setMaxAttempts(3);

        var manejador = new DefaultErrorHandler((registro, error) -> {
            // Métricas lee los mismos tópicos que tendencias y alertas: si también escribiera en la
            // DLT, cada registro malo quedaría dos veces. El que es dueño de la reacción lo descarta.
            if (error instanceof ListenerExecutionFailedException fallo && grupoMetricas.equals(fallo.getGroupId())) {
                log.warn("{}: registro no procesable de {} (partición {}, offset {}) se salta sin contarlo: {}",
                        grupoMetricas, registro.topic(), registro.partition(), registro.offset(), motivo(error));
                return;
            }
            log.error("Registro no procesable de {} (partición {}, offset {}, clave {}) → {}{}: {}",
                    registro.topic(), registro.partition(), registro.offset(), registro.key(),
                    registro.topic(), propiedades.getSufijoDlt(), motivo(error));
            recuperador.accept(registro, error);
        }, espera);
        // Sin arreglo: reintentar daría lo mismo y frenaría la partición. La deserialización y la
        // validación (@Valid) ya vienen de fábrica en esta lista.
        manejador.addNotRetryableExceptions(EventoNoProcesableException.class);
        manejador.setRetryListeners((registro, error, intento) ->
                log.warn("Intento {} de {} fallido para {} (partición {}, offset {}): {}", intento,
                        espera.getMaxAttempts() + 1, registro.topic(), registro.partition(), registro.offset(),
                        motivo(error)));
        return manejador;
    }

    /**
     * Repone en el MDC el identificador de correlación que viajó en la cabecera, así la línea del
     * consumidor se encuentra con la misma búsqueda que la petición que la originó.
     */
    @Bean
    public RecordInterceptor<Object, Object> correlacionDeRegistros() {
        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> registro,
                                                            Consumer<Object, Object> consumidor) {
                Header cabecera = registro.headers().lastHeader(CabecerasKafka.ID_PETICION);
                MDC.put(AtributosMensaje.CLAVE_MDC,
                        cabecera == null ? "sin-id" : new String(cabecera.value(), StandardCharsets.UTF_8));
                return registro;
            }

            @Override
            public void afterRecord(ConsumerRecord<Object, Object> registro, Consumer<Object, Object> consumidor) {
                MDC.remove(AtributosMensaje.CLAVE_MDC);
            }
        };
    }

    /** El mensaje de la causa de fondo: el error del listener viene envuelto. */
    static String motivo(Throwable error) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(error);
        String mensaje = causa.getMessage() == null ? causa.getClass().getSimpleName() : causa.getMessage();
        return mensaje.length() > 300 ? mensaje.substring(0, 300) + "…" : mensaje;
    }

    @SuppressWarnings("unchecked")
    private static <T> Serializer<T> castear(Serializer<?> serializador) {
        return (Serializer<T>) serializador;
    }
}
