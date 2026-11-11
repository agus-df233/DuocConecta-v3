package cl.duoc.duocconecta.analitica.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Cuántos registros de un tipo llegaron a un tópico en una hora. */
@Entity
@Table(name = "metrica_hora")
@Getter
@NoArgsConstructor
public class MetricaHora {

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false)
    private long cantidad;

    public MetricaHora(Clave clave) {
        this.clave = clave;
    }

    /** Suma un registro. */
    public void sumar() {
        cantidad++;
    }

    /** Hora (truncada), tópico lógico y tipo. */
    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Clave implements Serializable {

        private Instant hora;

        @Column(length = 20)
        private String topico;

        @Column(length = 40)
        private String tipo;
    }
}
