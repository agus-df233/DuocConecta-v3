package cl.duoc.duocconecta.comun.eventos.kafka;

import cl.duoc.duocconecta.comun.eventos.EventoDuocConecta;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lleva a Kafka los eventos de negocio, después del commit. Escucha el mismo
 * {@code publishEvent(...)} que ya usan SNS y RabbitMQ, así que el negocio no cambia nada.
 * También lleva las señales de uso, que no pasan por una transacción.
 */
public class PublicacionKafkaTrasConfirmar {

    private final PublicadorKafka publicador;

    public PublicacionKafkaTrasConfirmar(PublicadorKafka publicador) {
        this.publicador = publicador;
    }

    /** {@code fallbackExecution}: también publica si el evento se emite fuera de una transacción. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alConfirmar(EventoDuocConecta evento) {
        publicador.publicar(evento);
    }

    /** Una vista o una copia: se publica al tiro, porque no hay nada que confirmar. */
    @EventListener
    public void alUsar(SenalDeUso senal) {
        publicador.publicarUso(senal.interaccion(), senal.actividad(), senal.tipoRecurso(), senal.recursoId(),
                senal.usuarioId());
    }
}
