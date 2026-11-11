package cl.duoc.duocconecta.proyectos.config;

import cl.duoc.duocconecta.comun.eventos.ConfiguracionMensajeria;
import cl.duoc.duocconecta.comun.eventos.kafka.ConfiguracionKafka;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Publicación de eventos en SNS, RabbitMQ y Kafka. Vive aparte de la seguridad y del negocio: los
 * servicios solo hacen {@code publishEvent(...)} y no saben qué brokers existen.
 */
@Configuration
@Import({ConfiguracionMensajeria.class, ConfiguracionKafka.class})
public class ConfiguracionEventos {
}
