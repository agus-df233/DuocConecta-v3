package cl.duoc.duocconecta.comun.eventos.kafka;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Kafka propia de DuocConecta ({@code duocconecta.kafka.*}). La conexión, la
 * serialización y los grupos son de Spring ({@code spring.kafka.*}); aquí van los nombres de los
 * tópicos y cómo se crean. Ningún nombre de tópico se escribe en el código.
 */
@ConfigurationProperties(prefix = "duocconecta.kafka")
public class PropiedadesKafka {

    /** En falso nadie publica en Kafka y el negocio sigue igual: solo queda en el log. */
    private boolean habilitada = true;

    /** Servicio que publica; va en el DTO y en la cabecera {@code origen}. */
    private String origen;

    /** Tópicos por clave lógica: {@code interacciones} y {@code actividad}. */
    private Map<String, Topico> topicos = new LinkedHashMap<>();

    /** Sufijo de la DLT de cada tópico. Es el que usa por defecto {@code DeadLetterPublishingRecoverer}. */
    private String sufijoDlt = ".dlt";

    /** Cuánto se guarda lo que no se pudo procesar: 14 días, igual que las DLQ de SQS. */
    private long retencionDltMs = 1_209_600_000L;

    /** Particiones de cada tópico: una por broker, y hasta 3 consumidores en paralelo por grupo. */
    private int particiones = 3;

    /** Copias de cada partición: una en cada broker. */
    private short replicas = 3;

    /** Réplicas al día que exige una escritura con {@code acks=all}: con 3 copias se tolera perder 1. */
    private int minReplicasSincronizadas = 2;

    /** Nombre del tópico de una clave lógica. Falla al tiro si falta en el YAML. */
    public String topico(String clave) {
        Topico topico = topicos.get(clave);
        if (topico == null || topico.getNombre() == null || topico.getNombre().isBlank()) {
            throw new IllegalStateException("Falta duocconecta.kafka.topicos." + clave + ".nombre");
        }
        return topico.getNombre();
    }

    /** Nombre de la DLT de una clave lógica. */
    public String dlt(String clave) {
        return topico(clave) + sufijoDlt;
    }

    /** Un tópico de negocio y cómo se guarda. */
    public static class Topico {

        private String nombre;

        /** Cuánto se conserva cada registro. */
        private long retencionMs;

        /** Cada cuánto se cierra un segmento. La retención borra segmentos enteros, nunca registros sueltos. */
        private long segmentoMs;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public long getRetencionMs() {
            return retencionMs;
        }

        public void setRetencionMs(long retencionMs) {
            this.retencionMs = retencionMs;
        }

        public long getSegmentoMs() {
            return segmentoMs;
        }

        public void setSegmentoMs(long segmentoMs) {
            this.segmentoMs = segmentoMs;
        }
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Map<String, Topico> getTopicos() {
        return topicos;
    }

    public void setTopicos(Map<String, Topico> topicos) {
        this.topicos = topicos;
    }

    public String getSufijoDlt() {
        return sufijoDlt;
    }

    public void setSufijoDlt(String sufijoDlt) {
        this.sufijoDlt = sufijoDlt;
    }

    public long getRetencionDltMs() {
        return retencionDltMs;
    }

    public void setRetencionDltMs(long retencionDltMs) {
        this.retencionDltMs = retencionDltMs;
    }

    public int getParticiones() {
        return particiones;
    }

    public void setParticiones(int particiones) {
        this.particiones = particiones;
    }

    public short getReplicas() {
        return replicas;
    }

    public void setReplicas(short replicas) {
        this.replicas = replicas;
    }

    public int getMinReplicasSincronizadas() {
        return minReplicasSincronizadas;
    }

    public void setMinReplicasSincronizadas(int minReplicasSincronizadas) {
        this.minReplicasSincronizadas = minReplicasSincronizadas;
    }
}
