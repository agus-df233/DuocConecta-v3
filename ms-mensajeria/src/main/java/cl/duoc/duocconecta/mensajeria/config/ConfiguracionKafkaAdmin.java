package cl.duoc.duocconecta.mensajeria.config;

import cl.duoc.duocconecta.comun.eventos.kafka.PropiedadesKafka;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Administración de Kafka.
 *
 * <p>Declara la topología base: los dos tópicos de negocio y sus DLT, con 3 particiones, 3
 * réplicas, {@code min.insync.replicas = 2} y su retención. Al arrancar, {@link KafkaAdmin} crea
 * los que falten y no toca los que existen. Son los mismos valores de
 * {@code docker/kafka/topicos.sh}, que los crea desde la línea de comandos.</p>
 */
@Configuration
@EnableConfigurationProperties(PropiedadesKafka.class)
@ConditionalOnProperty(prefix = "duocconecta.kafka", name = "habilitada", havingValue = "true")
public class ConfiguracionKafkaAdmin {

    /** Cliente de administración, con esperas cortas: un clúster caído responde 503, no cuelga. */
    @Bean(destroyMethod = "close")
    public Admin clienteAdminKafka(KafkaAdmin kafkaAdmin) {
        Map<String, Object> configuracion = new HashMap<>(kafkaAdmin.getConfigurationProperties());
        configuracion.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configuracion.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);
        return Admin.create(configuracion);
    }

    /** Los tópicos de negocio y sus DLT. */
    @Bean
    public KafkaAdmin.NewTopics topologiaKafka(PropiedadesKafka propiedades) {
        List<NewTopic> topicos = new ArrayList<>();
        propiedades.getTopicos().forEach((clave, topico) -> {
            topicos.add(topico(propiedades, topico.getNombre(), topico.getRetencionMs(), topico.getSegmentoMs()));
            // La DLT se lee poco y se guarda más: 14 días para revisar con calma lo que falló.
            topicos.add(topico(propiedades, propiedades.dlt(clave), propiedades.getRetencionDltMs(), 86_400_000L));
        });
        return new KafkaAdmin.NewTopics(topicos.toArray(NewTopic[]::new));
    }

    private static NewTopic topico(PropiedadesKafka propiedades, String nombre, long retencionMs, long segmentoMs) {
        return TopicBuilder.name(nombre)
                .partitions(propiedades.getParticiones())
                .replicas(propiedades.getReplicas())
                .config("cleanup.policy", "delete")
                .config("retention.ms", Long.toString(retencionMs))
                .config("segment.ms", Long.toString(segmentoMs))
                .config("min.insync.replicas", Integer.toString(propiedades.getMinReplicasSincronizadas()))
                .build();
    }
}
