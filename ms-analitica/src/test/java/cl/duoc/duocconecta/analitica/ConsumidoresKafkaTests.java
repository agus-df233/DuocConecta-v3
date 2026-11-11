package cl.duoc.duocconecta.analitica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.duocconecta.analitica.repository.RepositorioAlertas;
import cl.duoc.duocconecta.analitica.service.ServicioTendencias;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los consumidores contra un Kafka de verdad (embebido): la reacción de cada grupo, los duplicados
 * y que lo que no tiene arreglo termine en la DLT con su motivo, sin frenar al resto.
 *
 * <p>Los registros se mandan como texto JSON, igual que los escribe el productor real.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers", topics = {
        ConsumidoresKafkaTests.INTERACCIONES, ConsumidoresKafkaTests.ACTIVIDAD,
        ConsumidoresKafkaTests.INTERACCIONES + ".dlt", ConsumidoresKafkaTests.ACTIVIDAD + ".dlt"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsumidoresKafkaTests {

    static final String INTERACCIONES = "duocconecta.contenido.interacciones";
    static final String ACTIVIDAD = "duocconecta.usuarios.actividad";
    private static final Duration ESPERA = Duration.ofSeconds(30);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ServicioTendencias tendencias;

    @Autowired
    private RepositorioAlertas alertas;

    @Autowired
    private MockMvc mockMvc;

    private KafkaTemplate<String, String> productor;
    private Consumer<String, String> lectorDlt;

    @BeforeAll
    void preparar() {
        productor = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                KafkaTestUtils.producerProps(broker), new StringSerializer(), new StringSerializer()));
        lectorDlt = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps(broker, "prueba-dlt", false),
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromEmbeddedTopics(lectorDlt, INTERACCIONES + ".dlt", ACTIVIDAD + ".dlt");
    }

    @AfterAll
    void cerrar() {
        lectorDlt.close();
    }

    @Test
    @DisplayName("Dos vistas y un comentario: la tendencia suma 1 + 1 + 3")
    void tendenciaSumaPorPeso() {
        String proyecto = UUID.randomUUID().toString();
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "VISTA", proyecto, 1));
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "VISTA", proyecto, 1));
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "COMENTARIO", proyecto, 1));

        await().atMost(ESPERA).untilAsserted(() -> {
            var tendencia = tendencias.deRecurso(proyecto);
            assertThat(tendencia.puntaje()).isEqualTo(5);
            assertThat(tendencia.conteos()).containsEntry(TipoInteraccion.VISTA, 2L);
        });
    }

    @Test
    @DisplayName("El mismo idEvento entregado dos veces se cuenta una sola vez")
    void duplicadoSeCuentaUnaVez() {
        String proyecto = UUID.randomUUID().toString();
        String json = interaccion(UUID.randomUUID().toString(), "VISTA", proyecto, 1);
        enviar(INTERACCIONES, proyecto, json);
        enviar(INTERACCIONES, proyecto, json);
        String testigo = UUID.randomUUID().toString();
        // Un registro posterior de la misma partición: cuando llega, los dos anteriores ya pasaron.
        enviar(INTERACCIONES, proyecto, interaccion(testigo, "COMENTARIO", proyecto, 1));

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(tendencias.deRecurso(proyecto).conteos()).containsEntry(TipoInteraccion.COMENTARIO, 1L));
        assertThat(tendencias.deRecurso(proyecto).conteos()).containsEntry(TipoInteraccion.VISTA, 1L);
    }

    @Test
    @DisplayName("Ocultar un proyecto lo saca de la tendencia; restaurarlo lo devuelve")
    void ocultarYRestaurar() {
        String proyecto = UUID.randomUUID().toString();
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "SOLICITUD_COLABORACION", proyecto, 1));
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "OCULTADO", proyecto, 1));

        await().atMost(ESPERA).untilAsserted(() -> {
            assertThat(tendencias.deRecurso(proyecto).conteos()).containsEntry(TipoInteraccion.SOLICITUD_COLABORACION, 1L);
            assertThat(tendencias.deRecurso(proyecto).puntaje()).isZero();
            assertThat(tendencias.tendencias(TipoRecurso.PROYECTO, 50)).noneMatch(t -> t.recursoId().equals(proyecto));
        });

        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "RESTAURADO", proyecto, 1));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(tendencias.tendencias(TipoRecurso.PROYECTO, 50)).anyMatch(t -> t.recursoId().equals(proyecto)));
    }

    @Test
    @DisplayName("Tres comentarios seguidos de la misma persona crean una sola alerta")
    void rafagaCreaAlerta() {
        String persona = "oid-" + UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            enviar(ACTIVIDAD, persona, actividad(UUID.randomUUID().toString(), "COMENTARIO", persona));
        }

        await().atMost(ESPERA).untilAsserted(() -> {
            var deLaPersona = alertas.findAll().stream().filter(a -> a.getUsuarioId().equals(persona)).toList();
            assertThat(deLaPersona).hasSize(1);
            assertThat(deLaPersona.get(0).getCantidad()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("JSON ilegible, DTO inválido, versión desconocida o clave cambiada: a la DLT con su motivo")
    void loQueNoTieneArregloVaALaDlt() {
        String proyecto = UUID.randomUUID().toString();
        enviar(INTERACCIONES, proyecto, "{esto no es json");
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "VISTA", "", 1));
        enviar(INTERACCIONES, proyecto, interaccion(UUID.randomUUID().toString(), "VISTA", proyecto, 99));
        enviar(INTERACCIONES, "otra-clave", interaccion(UUID.randomUUID().toString(), "VISTA", proyecto, 1));
        String testigo = UUID.randomUUID().toString();
        enviar(INTERACCIONES, proyecto, interaccion(testigo, "COMENTARIO", proyecto, 1));

        // El registro bueno que venía detrás se procesó: los malos no frenaron la partición.
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(tendencias.deRecurso(proyecto).conteos()).containsEntry(TipoInteraccion.COMENTARIO, 1L));

        List<ConsumerRecord<String, String>> descartados = new java.util.ArrayList<>();
        await().atMost(ESPERA).untilAsserted(() -> {
            KafkaTestUtils.getRecords(lectorDlt, Duration.ofSeconds(2)).forEach(descartados::add);
            assertThat(descartados).filteredOn(r -> r.topic().equals(INTERACCIONES + ".dlt")).hasSizeGreaterThanOrEqualTo(4);
        });
        assertThat(descartados).allSatisfy(r -> assertThat(r.headers().lastHeader("motivo-descarte")).isNotNull());
        assertThat(descartados).anySatisfy(r -> assertThat(motivo(r)).contains("otra-clave"));
        assertThat(descartados).anySatisfy(r -> assertThat(r.value()).isEqualTo("{esto no es json"));
    }

    @Test
    @DisplayName("Las alertas son solo para ADMINISTRADOR; la tendencia, para cualquier rol")
    void seguridad() throws Exception {
        mockMvc.perform(get("/api/v1/analitica/admin/alertas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/analitica/admin/alertas").with(token("ROLE_ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/analitica/admin/alertas").with(token("ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analitica/tendencias").with(token("ROLE_ESTUDIANTE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/analitica/admin/consumidores").with(token("ROLE_ESTUDIANTE", "ROLE_ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    private void enviar(String topico, String clave, String json) {
        var registro = new ProducerRecord<>(topico, clave, json);
        registro.headers().add("X-Request-Id", "prueba".getBytes(StandardCharsets.UTF_8));
        productor.send(registro).join();
    }

    private static String interaccion(String idEvento, String tipo, String recursoId, int version) {
        return """
                {"idEvento":"%s","tipo":"%s","tipoRecurso":"PROYECTO","recursoId":"%s",
                 "ocurridoEn":"%s","origen":"ms-proyectos","version":%d}"""
                .formatted(idEvento, tipo, recursoId, Instant.now(), version);
    }

    private static String actividad(String idEvento, String tipo, String usuarioId) {
        return """
                {"idEvento":"%s","tipo":"%s","usuarioId":"%s","tipoRecurso":"PROYECTO","recursoId":"p-1",
                 "ocurridoEn":"%s","origen":"ms-proyectos","version":1}"""
                .formatted(idEvento, tipo, usuarioId, Instant.now());
    }

    private static String motivo(ConsumerRecord<String, String> registro) {
        return new String(registro.headers().lastHeader("motivo-descarte").value(), StandardCharsets.UTF_8);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor token(String... roles) {
        return jwt().jwt(t -> t.claim("oid", "oid-admin").claim("email", "admin@duocuc.cl").claim("name", "Admin"))
                .authorities(java.util.Arrays.stream(roles).map(r -> (org.springframework.security.core.GrantedAuthority) new SimpleGrantedAuthority(r)).toList());
    }
}
