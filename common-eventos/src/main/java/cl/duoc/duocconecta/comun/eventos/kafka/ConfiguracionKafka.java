package cl.duoc.duocconecta.comun.eventos.kafka;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Productor de Kafka. Cada servicio que publica la incorpora con
 * {@code @Import(ConfiguracionKafka.class)}, y la configuración del cliente (brokers,
 * serializadores, acks…) la trae con {@code spring.config.import: classpath:duocconecta-kafka.yml}.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesKafka.class)
public class ConfiguracionKafka {

    @Bean
    public TraductorEventosKafka traductorEventosKafka(PropiedadesKafka propiedades) {
        return new TraductorEventosKafka(propiedades.getOrigen());
    }

    @Bean
    public PublicadorKafka publicadorKafka(KafkaTemplate<String, Object> plantilla, PropiedadesKafka propiedades,
                                           TraductorEventosKafka traductor) {
        return new PublicadorKafka(plantilla, propiedades, traductor);
    }

    @Bean
    public PublicacionKafkaTrasConfirmar publicacionKafkaTrasConfirmar(PublicadorKafka publicador) {
        return new PublicacionKafkaTrasConfirmar(publicador);
    }
}
