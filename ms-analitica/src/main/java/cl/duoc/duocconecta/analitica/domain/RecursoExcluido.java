package cl.duoc.duocconecta.analitica.domain;

import cl.duoc.duocconecta.comun.eventos.kafka.TipoInteraccion;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Un recurso oculto o eliminado: sus interacciones se guardan, pero no sale en la tendencia. */
@Entity
@Table(name = "recurso_excluido")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecursoExcluido {

    @Id
    @Column(name = "recurso_id", length = 100)
    private String recursoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private TipoRecurso tipoRecurso;

    /** {@code OCULTADO} o {@code ELIMINADO}: solo el primero se revierte con {@code RESTAURADO}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoInteraccion motivo;

    @Column(nullable = false)
    private Instant desde;
}
