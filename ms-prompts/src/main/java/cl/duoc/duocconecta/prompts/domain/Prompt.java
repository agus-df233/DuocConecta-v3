package cl.duoc.duocconecta.prompts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Un prompt de la biblioteca: el texto que se copia y lo que hace falta para encontrarlo. */
@Entity
@Table(name = "prompt")
@Getter
@Setter
@NoArgsConstructor
public class Prompt {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titulo;

    /** Para qué sirve: la línea de la tarjeta. */
    @Column(nullable = false, length = 500)
    private String proposito;

    /** El texto del prompt, tal cual se copia. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaPrompt categoria;

    @Column(length = 120)
    private String carrera;

    /** Herramienta de IA con que se probó. Texto libre: ninguna lista cerrada queda al día. */
    @Column(length = 80)
    private String herramienta;

    @Column(name = "autor_id", nullable = false, length = 100)
    private String autorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibilidad visibilidad;

    /** De qué prompt es copia. Si el original se borra, queda en nulo y la copia sigue. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Prompt origen;

    /** Cuántas copias se hicieron de este prompt. */
    @Column(nullable = false)
    private int bifurcaciones;

    @Column(name = "suma_estrellas", nullable = false)
    private int sumaEstrellas;

    @Column(name = "cantidad_valoraciones", nullable = false)
    private int cantidadValoraciones;

    /** Oculto por moderación: aparte de la visibilidad, para que el autor no lo revierta. */
    @Column(nullable = false)
    private boolean oculto;

    @Column(name = "motivo_ocultamiento", length = 500)
    private String motivoOcultamiento;

    @Column(name = "ocultado_por", length = 100)
    private String ocultadoPor;

    @Column(name = "ocultado_en")
    private Instant ocultadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    void alCrear() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    /** Lo ve quien lo publicó, siempre; el resto, solo si es público y no está oculto. */
    public boolean esVisiblePara(String usuarioId) {
        return autorId.equals(usuarioId) || (visibilidad == Visibilidad.PUBLICO && !oculto);
    }

    /** Promedio de estrellas con un decimal; 0 sin valoraciones. */
    public double promedio() {
        return cantidadValoraciones == 0 ? 0 : Math.round(10.0 * sumaEstrellas / cantidadValoraciones) / 10.0;
    }

    /** Suma una valoración nueva, o reemplaza la anterior de la misma persona. */
    public void aplicarValoracion(Integer anterior, int nueva) {
        if (anterior == null) {
            cantidadValoraciones++;
            sumaEstrellas += nueva;
        } else {
            sumaEstrellas += nueva - anterior;
        }
    }

    /** Alguien hizo su copia. */
    public void contarBifurcacion() {
        bifurcaciones++;
    }

    /** Lo oculta un administrador. */
    public void ocultar(String motivo, String administrador) {
        oculto = true;
        motivoOcultamiento = motivo;
        ocultadoPor = administrador;
        ocultadoEn = Instant.now();
    }

    /** Lo devuelve a la biblioteca. */
    public void mostrar() {
        oculto = false;
        motivoOcultamiento = null;
        ocultadoPor = null;
        ocultadoEn = null;
    }
}
