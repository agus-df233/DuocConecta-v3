package cl.duoc.duocconecta.analitica.domain;

import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Conteo de un tipo de interacción sobre un recurso en un día. */
@Entity
@Table(name = "interaccion_diaria")
@Getter
@NoArgsConstructor
public class InteraccionDiaria {

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false)
    private long cantidad;

    public InteraccionDiaria(Clave clave) {
        this.clave = clave;
    }

    /** Suma una interacción. */
    public void sumar() {
        cantidad++;
    }

    /** Recurso, día y tipo: lo que identifica un conteo. */
    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Clave implements Serializable {

        @Enumerated(EnumType.STRING)
        @Column(name = "tipo_recurso", length = 20)
        private TipoRecurso tipoRecurso;

        @Column(name = "recurso_id", length = 100)
        private String recursoId;

        private LocalDate dia;

        @Enumerated(EnumType.STRING)
        @Column(length = 40)
        private TipoInteraccion tipo;
    }
}
