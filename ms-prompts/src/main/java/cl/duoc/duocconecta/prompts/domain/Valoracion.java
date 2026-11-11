package cl.duoc.duocconecta.prompts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Las estrellas que una persona le dio a un prompt. Una por persona: la clave lo asegura. */
@Entity
@Table(name = "valoracion")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Valoracion {

    @EmbeddedId
    private Clave clave;

    @Column(nullable = false)
    private short estrellas;

    @Column(name = "valorado_en", nullable = false)
    private Instant valoradoEn;

    /** Cambia las estrellas de una valoración existente. */
    public void cambiar(short nuevas) {
        this.estrellas = nuevas;
        this.valoradoEn = Instant.now();
    }

    /** Prompt y persona. */
    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Clave implements Serializable {

        @Column(name = "prompt_id")
        private UUID promptId;

        @Column(name = "usuario_id", length = 100)
        private String usuarioId;
    }
}
