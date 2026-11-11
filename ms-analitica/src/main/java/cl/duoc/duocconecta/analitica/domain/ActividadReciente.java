package cl.duoc.duocconecta.analitica.domain;

import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Una acción de una persona, guardada solo 24 h para contar ráfagas. */
@Entity
@Table(name = "actividad_reciente")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActividadReciente {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoActividad tipo;

    @Column(name = "recurso_id", length = 100)
    private String recursoId;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;
}
