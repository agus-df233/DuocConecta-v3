package cl.duoc.duocconecta.comun.eventos.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import cl.duoc.duocconecta.comun.eventos.TiposDeEvento;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Qué registros de Kafka sale de cada evento de negocio, con qué clave y en qué tópico. */
class TraductorEventosKafkaTest {

    private final TraductorEventosKafka traductor = new TraductorEventosKafka("ms-prueba");

    @Test
    @DisplayName("Un comentario va a los dos tópicos: clave del proyecto en uno y de la persona en el otro")
    void comentarioVaALosDosTopicos() {
        var evento = EventoDuocConecta.nuevo(TiposDeEvento.PROYECTO_COMENTADO, "oid-ana", "oid-dueno",
                "comentario-1", "proyecto-1", null);

        List<RegistroKafka> registros = traductor.traducir(evento);

        assertThat(registros).hasSize(2);
        RegistroKafka interaccion = registros.get(0);
        assertThat(interaccion.claveTopico()).isEqualTo(RegistroKafka.INTERACCIONES);
        assertThat(interaccion.clave()).isEqualTo("proyecto-1");
        var dto = (InteraccionContenido) interaccion.valor();
        assertThat(dto.tipo()).isEqualTo(TipoInteraccion.COMENTARIO);
        assertThat(dto.tipoRecurso()).isEqualTo(TipoRecurso.PROYECTO);
        assertThat(dto.idEvento()).isEqualTo(evento.idEvento());
        assertThat(dto.origen()).isEqualTo("ms-prueba");

        RegistroKafka actividad = registros.get(1);
        assertThat(actividad.claveTopico()).isEqualTo(RegistroKafka.ACTIVIDAD);
        assertThat(actividad.clave()).isEqualTo("oid-ana");
        assertThat(((ActividadUsuario) actividad.valor()).recursoId()).isEqualTo("proyecto-1");
    }

    @Test
    @DisplayName("Ocultar un proyecto cambia su estado en la tendencia, pero no es actividad de nadie")
    void ocultarSoloVaAInteracciones() {
        var evento = EventoDuocConecta.nuevo(TiposDeEvento.PROYECTO_OCULTADO, "oid-admin", "oid-dueno",
                "proyecto-1", "proyecto-1", "spam");

        assertThat(traductor.traducir(evento)).singleElement()
                .satisfies(r -> assertThat(((InteraccionContenido) r.valor()).tipo()).isEqualTo(TipoInteraccion.OCULTADO));
    }

    @Test
    @DisplayName("Una solicitud directa a una persona, sin proyecto, cuenta solo como actividad")
    void solicitudSinProyecto() {
        var evento = EventoDuocConecta.nuevo(TiposDeEvento.COLABORACION_SOLICITADA, "oid-ana", "oid-beto",
                "solicitud-1", null, null);

        assertThat(traductor.traducir(evento)).singleElement().satisfies(r -> {
            assertThat(r.claveTopico()).isEqualTo(RegistroKafka.ACTIVIDAD);
            assertThat(((ActividadUsuario) r.valor()).recursoId()).isNull();
        });
    }

    @Test
    @DisplayName("Bifurcar un prompt suma al original, que es el que demostró ser útil")
    void bifurcarSumaAlOriginal() {
        var evento = EventoDuocConecta.nuevo(TiposDeEvento.PROMPT_BIFURCADO, "oid-ana", "oid-autor",
                "prompt-original", null, "prompt-copia");

        List<RegistroKafka> registros = traductor.traducir(evento);

        assertThat(registros).extracting(RegistroKafka::clave).containsExactly("prompt-original", "oid-ana");
        assertThat(((InteraccionContenido) registros.get(0).valor()).tipoRecurso()).isEqualTo(TipoRecurso.PROMPT);
    }

    @Test
    @DisplayName("Lo que no le sirve a la analítica no se traduce")
    void edicionesNoSeTraducen() {
        assertThat(traductor.traducir(EventoDuocConecta.nuevo(TiposDeEvento.PROYECTO_ACTUALIZADO,
                "oid-ana", null, "p", "p", null))).isEmpty();
        assertThat(traductor.traducir(EventoDuocConecta.nuevo(TiposDeEvento.COLABORACION_ACEPTADA,
                "oid-ana", "oid-beto", "s", "p", null))).isEmpty();
    }

    @Test
    @DisplayName("Una señal de uso sin persona (por ejemplo, anónima) va solo a interacciones")
    void usoSinPersona() {
        var registros = traductor.uso(TipoInteraccion.VISTA, TipoActividad.VISTA, TipoRecurso.PROYECTO,
                "proyecto-1", null, "id-1", java.time.Instant.now());

        assertThat(registros).extracting(RegistroKafka::claveTopico).containsExactly(RegistroKafka.INTERACCIONES);
    }
}
