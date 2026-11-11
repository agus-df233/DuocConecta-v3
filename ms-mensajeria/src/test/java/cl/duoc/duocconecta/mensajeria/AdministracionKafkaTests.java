package cl.duoc.duocconecta.mensajeria;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * La API de administración de Kafka contra un broker embebido: los tópicos base se crean solos, se
 * cambia una retención, se protege la topología y se lee el lag de un grupo.
 */
@SpringBootTest(properties = {"duocconecta.kafka.habilitada=true",
        // Un broker embebido: los tópicos base nacen con RF 1. El RF 3 se prueba contra el clúster
        // de docker-compose (el embebido de 3 nodos en KRaft no es estable en un test).
        "duocconecta.kafka.replicas=1", "duocconecta.kafka.min-replicas-sincronizadas=1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(count = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers", brokerProperties = {
        "offsets.topic.num.partitions=3", "offsets.topic.replication.factor=1", "group.initial.rebalance.delay.ms=0"})
class AdministracionKafkaTests {

    private static final String BASE = "/api/v1/mensajeria/kafka";
    private static final String INTERACCIONES = "duocconecta.contenido.interacciones";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    @DisplayName("Al arrancar crea los 4 tópicos base con 3 particiones y su retención")
    void topologiaBase() throws Exception {
        mockMvc.perform(get(BASE + "/cluster").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokers.length()").value(1))
                .andExpect(jsonPath("$.brokers[0].controlador").value(true));
        mockMvc.perform(get(BASE + "/topicos/" + INTERACCIONES).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protegido").value(true))
                .andExpect(jsonPath("$.replicacion").value(1))
                .andExpect(jsonPath("$.particiones.length()").value(3))
                .andExpect(jsonPath("$.particiones[0].sincronizadas.length()").value(1))
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("604800000"))
                .andExpect(jsonPath("$.configuracion['segment.ms']").value("86400000"))
                .andExpect(jsonPath("$.configuracion['cleanup.policy']").value("delete"));
        mockMvc.perform(get(BASE + "/topicos").with(administrador()))
                .andExpect(jsonPath("$[*].nombre", Matchers.hasItems(INTERACCIONES, INTERACCIONES + ".dlt",
                        "duocconecta.usuarios.actividad", "duocconecta.usuarios.actividad.dlt")));
    }

    @Test
    @DisplayName("Crear, cambiar la retención, aumentar particiones y borrar un tópico propio")
    void cicloDeUnTopico() throws Exception {
        String topico = "duocconecta.pruebas.ciclo";
        mockMvc.perform(post(BASE + "/topicos").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","particiones":3,"replicacion":1,
                                 "configuracion":{"retention.ms":"3600000","cleanup.policy":"delete"}}""".formatted(topico)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protegido").value(false))
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("3600000"));

        mockMvc.perform(post(BASE + "/topicos").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\",\"particiones\":3,\"replicacion\":1}".formatted(topico)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(BASE + "/topicos/" + topico + "/configuracion").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fijar\":{\"retention.ms\":\"7200000\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuracion['retention.ms']").value("7200000"));

        mockMvc.perform(post(BASE + "/topicos/" + topico + "/particiones").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.particiones.length()").value(6));

        mockMvc.perform(delete(BASE + "/topicos/" + topico).with(administrador()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("La topología base no se borra ni cambia de particiones; lo inválido responde 400")
    void proteccionYValidacion() throws Exception {
        mockMvc.perform(delete(BASE + "/topicos/" + INTERACCIONES).with(administrador()))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/topicos/" + INTERACCIONES + "/particiones").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"total\":6}"))
                .andExpect(status().isConflict());
        // La retención de un tópico base sí se ajusta: cambia cuánto se guarda, no el orden.
        mockMvc.perform(patch(BASE + "/topicos/" + INTERACCIONES + "/configuracion").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fijar\":{\"retention.ms\":\"604800000\"}}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/topicos/__consumer_offsets").with(administrador()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/topicos").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"duocconecta.pruebas.rf\",\"particiones\":3,\"replicacion\":2}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/topicos/" + INTERACCIONES + "/configuracion").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fijar\":{\"unclean.leader.election.enable\":\"true\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(BASE + "/topicos/" + INTERACCIONES + "/configuracion").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fijar\":{\"cleanup.policy\":\"borrar\"}}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/topicos").with(estudiante()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El lag de un grupo es lo escrito menos lo confirmado; con el grupo vacío se puede releer")
    void lagYOffsets() throws Exception {
        String topico = "duocconecta.pruebas.lag";
        String grupo = "prueba-lag";
        mockMvc.perform(post(BASE + "/topicos").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"%s\",\"particiones\":1,\"replicacion\":1}".formatted(topico)))
                .andExpect(status().isCreated());

        var productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker),
                new StringSerializer(), new StringSerializer()));
        for (int i = 0; i < 5; i++) {
            productor.send(new ProducerRecord<>(topico, "clave", "registro " + i)).join();
        }
        // Un consumidor lee 2 y confirma, y se va: quedan 3 pendientes.
        Map<String, Object> propiedades = KafkaTestUtils.consumerProps(broker, grupo, false);
        propiedades.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
        propiedades.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumidor = new DefaultKafkaConsumerFactory<>(propiedades,
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumidor.subscribe(List.of(topico));
            var leidos = KafkaTestUtils.getRecords(consumidor, java.time.Duration.ofSeconds(10), 2);
            consumidor.commitSync(Map.of(new org.apache.kafka.common.TopicPartition(topico, 0),
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(leidos.count())));
        }

        mockMvc.perform(get(BASE + "/grupos/" + grupo).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMPTY"))
                .andExpect(jsonPath("$.particiones[0].confirmado").value(2))
                .andExpect(jsonPath("$.particiones[0].offsetFinal").value(5))
                .andExpect(jsonPath("$.lagTotal").value(3));

        mockMvc.perform(post(BASE + "/grupos/" + grupo + "/offsets").with(administrador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destino\":\"INICIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lagTotal").value(5));

        mockMvc.perform(get(BASE + "/grupos").with(administrador()))
                .andExpect(jsonPath("$[*].id", Matchers.hasItem(grupo)));
    }

    private static RequestPostProcessor administrador() {
        return token("ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR");
    }

    private static RequestPostProcessor estudiante() {
        return token("ROLE_ESTUDIANTE");
    }

    private static RequestPostProcessor token(String... roles) {
        List<GrantedAuthority> permisos = java.util.Arrays.stream(roles)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r)).toList();
        return jwt().jwt(t -> t.claim("oid", "oid-admin").claim("email", "admin@duocuc.cl").claim("name", "Admin"))
                .authorities(permisos);
    }
}
