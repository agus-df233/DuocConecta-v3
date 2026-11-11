package cl.duoc.duocconecta.mensajeria.exception;

/** Kafka no respondió a tiempo: los brokers están caídos o no se alcanzan. */
public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
