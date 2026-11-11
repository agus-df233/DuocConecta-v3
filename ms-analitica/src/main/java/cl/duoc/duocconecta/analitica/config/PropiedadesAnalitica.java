package cl.duoc.duocconecta.analitica.config;

import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de la analítica ({@code duocconecta.analitica.*}): cuánto pesa cada interacción y cuándo
 * una ráfaga es una alerta. Se ajustan en el YAML o por variable de entorno, sin recompilar.
 */
@ConfigurationProperties(prefix = "duocconecta.analitica")
public class PropiedadesAnalitica {

    /** Días que cuenta la tendencia. Coincide con la retención del tópico de interacciones. */
    private int ventanaTendenciaDias = 7;

    /** Horas que se guarda la actividad de una persona. Coincide con la retención de su tópico. */
    private int retencionActividadHoras = 24;

    /** Peso de cada interacción, por tipo de recurso. Lo que no está, no suma. */
    private Map<TipoRecurso, Map<TipoInteraccion, Integer>> pesos = new EnumMap<>(TipoRecurso.class);

    /** Reglas de alerta por tipo de actividad. Lo que no está, nunca alerta (por ejemplo, las vistas). */
    private Map<TipoActividad, Regla> alertas = new EnumMap<>(TipoActividad.class);

    /** Cuánto pesa una interacción sobre un tipo de recurso. */
    public int peso(TipoRecurso tipoRecurso, TipoInteraccion tipo) {
        return pesos.getOrDefault(tipoRecurso, Map.of()).getOrDefault(tipo, 0);
    }

    /** "Más de {@code umbral} veces en {@code ventanaMinutos} minutos" es una ráfaga. */
    public static class Regla {

        private int umbral;
        private int ventanaMinutos;

        public int getUmbral() {
            return umbral;
        }

        public void setUmbral(int umbral) {
            this.umbral = umbral;
        }

        public int getVentanaMinutos() {
            return ventanaMinutos;
        }

        public void setVentanaMinutos(int ventanaMinutos) {
            this.ventanaMinutos = ventanaMinutos;
        }
    }

    public int getVentanaTendenciaDias() {
        return ventanaTendenciaDias;
    }

    public void setVentanaTendenciaDias(int ventanaTendenciaDias) {
        this.ventanaTendenciaDias = ventanaTendenciaDias;
    }

    public int getRetencionActividadHoras() {
        return retencionActividadHoras;
    }

    public void setRetencionActividadHoras(int retencionActividadHoras) {
        this.retencionActividadHoras = retencionActividadHoras;
    }

    public Map<TipoRecurso, Map<TipoInteraccion, Integer>> getPesos() {
        return pesos;
    }

    public void setPesos(Map<TipoRecurso, Map<TipoInteraccion, Integer>> pesos) {
        this.pesos = pesos;
    }

    public Map<TipoActividad, Regla> getAlertas() {
        return alertas;
    }

    public void setAlertas(Map<TipoActividad, Regla> alertas) {
        this.alertas = alertas;
    }
}
