package cl.duoc.duocconecta.comun.eventos.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.duocconecta.comun.eventos.AtributosMensaje;
import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import cl.duoc.duocconecta.comun.eventos.TiposDeEvento;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

/** Envío a Kafka sin broker: tópico, clave, cabeceras, y que un fallo no salga de aquí. */
class PublicadorKafkaTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> plantilla = mock(KafkaTemplate.class);
    private final PropiedadesKafka propiedades = propiedades();
    private final PublicadorKafka publicador =
            new PublicadorKafka(plantilla, propiedades, new TraductorEventosKafka("ms-prueba"));

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Cada registro sale a su tópico, con su clave y las cabeceras para rastrearlo")
    @SuppressWarnings("unchecked")
    void enviaConClaveYCabeceras() {
        when(plantilla.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());
        MDC.put(AtributosMensaje.CLAVE_MDC, "a3f19c2b");

        publicador.publicar(EventoDuocConecta.nuevo(TiposDeEvento.PROYECTO_COMENTADO, "oid-ana", "oid-dueno",
                "comentario-1", "proyecto-1", null));

        ArgumentCaptor<ProducerRecord<String, Object>> captura = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(plantilla, org.mockito.Mockito.times(2)).send(captura.capture());
        List<ProducerRecord<String, Object>> enviados = captura.getAllValues();
        assertThat(enviados).extracting(ProducerRecord::topic)
                .containsExactly("duocconecta.contenido.interacciones", "duocconecta.usuarios.actividad");
        assertThat(enviados).extracting(ProducerRecord::key).containsExactly("proyecto-1", "oid-ana");
        var primero = enviados.get(0);
        assertThat(texto(primero, CabecerasKafka.ID_PETICION)).isEqualTo("a3f19c2b");
        assertThat(texto(primero, CabecerasKafka.TIPO)).isEqualTo("COMENTARIO");
        assertThat(texto(primero, CabecerasKafka.ORIGEN)).isEqualTo("ms-prueba");
    }

    @Test
    @DisplayName("Sin brokers, send() falla en este hilo: se registra y el negocio sigue")
    @SuppressWarnings("unchecked")
    void unFalloNoSaleDelPublicador() {
        when(plantilla.send(any(ProducerRecord.class))).thenThrow(new KafkaException("sin metadata"));

        publicador.publicarUso(TipoInteraccion.VISTA, TipoActividad.VISTA, TipoRecurso.PROYECTO, "p-1", "oid-ana");
        // Llegar aquí sin excepción es la prueba.
    }

    @Test
    @DisplayName("Con Kafka deshabilitada no se envía nada")
    @SuppressWarnings("unchecked")
    void deshabilitada() {
        propiedades.setHabilitada(false);

        publicador.publicarUso(TipoInteraccion.VISTA, TipoActividad.VISTA, TipoRecurso.PROYECTO, "p-1", "oid-ana");

        verify(plantilla, never()).send(any(ProducerRecord.class));
    }

    private static String texto(ProducerRecord<String, Object> registro, String cabecera) {
        return new String(registro.headers().lastHeader(cabecera).value(), StandardCharsets.UTF_8);
    }

    private static PropiedadesKafka propiedades() {
        var propiedades = new PropiedadesKafka();
        propiedades.setOrigen("ms-prueba");
        var interacciones = new PropiedadesKafka.Topico();
        interacciones.setNombre("duocconecta.contenido.interacciones");
        var actividad = new PropiedadesKafka.Topico();
        actividad.setNombre("duocconecta.usuarios.actividad");
        propiedades.getTopicos().put("interacciones", interacciones);
        propiedades.getTopicos().put("actividad", actividad);
        return propiedades;
    }
}
