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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una ráfaga de actividad: alguien hizo lo mismo más veces de lo normal en pocos minutos. La
 * revisa el administrador; la plataforma no bloquea a nadie sola.
 */
@Entity
@Table(name = "alerta")
@Getter
@NoArgsConstructor
public class Alerta {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, length = 100)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoActividad tipo;

    /** Cuántas veces lo hizo dentro de la ventana. */
    @Column(nullable = false)
    private int cantidad;

    @Column(name = "ventana_minutos", nullable = false)
    private int ventanaMinutos;

    /** El último recurso sobre el que actuó, para saber dónde mirar. */
    @Column(name = "recurso_id", length = 100)
    private String recursoId;

    @Column(nullable = false)
    private Instant desde;

    @Column(nullable = false)
    private Instant hasta;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private boolean revisada;

    public Alerta(String usuarioId, TipoActividad tipo, int cantidad, int ventanaMinutos, String recursoId,
                  Instant desde, Instant hasta) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.ventanaMinutos = ventanaMinutos;
        this.recursoId = recursoId;
        this.desde = desde;
        this.hasta = hasta;
        this.creadaEn = Instant.now();
    }

    /** La ráfaga sigue: la misma alerta se extiende en vez de crear otra. */
    public void extender(int nuevaCantidad, String ultimoRecurso, Instant nuevoHasta) {
        this.cantidad = Math.max(cantidad, nuevaCantidad);
        this.recursoId = ultimoRecurso;
        this.hasta = nuevoHasta;
    }

    /** El administrador ya la miró. */
    public void marcarRevisada() {
        this.revisada = true;
    }
}
