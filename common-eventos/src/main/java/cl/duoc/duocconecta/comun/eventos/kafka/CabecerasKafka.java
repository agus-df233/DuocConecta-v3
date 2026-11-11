package cl.duoc.duocconecta.comun.eventos.kafka;

/**
 * Cabeceras de los registros de Kafka. El cuerpo es el DTO; lo que sirve para rastrear va aquí,
 * igual que los atributos de SNS y las cabeceras de RabbitMQ.
 */
public final class CabecerasKafka {

    /** Identificador de correlación de la petición que originó el registro. */
    public static final String ID_PETICION = "X-Request-Id";
    /** Microservicio que lo publicó. */
    public static final String ORIGEN = "origen";
    /** El tipo del DTO ({@code VISTA}, {@code COMENTARIO}…), para filtrar en Kafka UI sin abrir el cuerpo. */
    public static final String TIPO = "tipo";

    private CabecerasKafka() {
    }
}
