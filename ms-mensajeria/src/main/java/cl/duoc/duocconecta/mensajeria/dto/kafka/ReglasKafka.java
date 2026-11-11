package cl.duoc.duocconecta.mensajeria.dto.kafka;

import java.util.Set;

/** Reglas de los nombres y de las configuraciones que la API acepta para Kafka. */
public final class ReglasKafka {

    /** Solo tópicos de la plataforma: así nadie toca {@code __consumer_offsets} desde aquí. */
    public static final String PREFIJO = "duocconecta.";

    /** Palabras separadas por punto o guion, en minúsculas. Kafka acepta hasta 249 caracteres. */
    public static final String PATRON = "^duocconecta\\.[a-z0-9]+([.-][a-z0-9]+)*$";

    /** Configuraciones de tópico que se pueden fijar o cambiar desde la API. */
    public static final Set<String> CONFIGURACIONES = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "segment.ms",
            "min.insync.replicas", "max.message.bytes");

    /** Valores válidos de {@code cleanup.policy}. */
    public static final Set<String> POLITICAS_DE_LIMPIEZA = Set.of("delete", "compact", "compact,delete");

    private ReglasKafka() {
    }
}
