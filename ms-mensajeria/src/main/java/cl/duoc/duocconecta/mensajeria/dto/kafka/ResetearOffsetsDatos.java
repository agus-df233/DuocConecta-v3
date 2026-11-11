package cl.duoc.duocconecta.mensajeria.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * A dónde mover los offsets de un grupo, por ejemplo para releer y reconstruir una vista.
 *
 * @param destino  {@code INICIO}, {@code FIN} o {@code FECHA}
 * @param fecha    con {@code FECHA}: desde qué instante releer
 * @param topico   opcional: solo ese tópico; vacío = todos los del grupo
 */
public record ResetearOffsetsDatos(
        @NotNull @Schema(example = "INICIO") Destino destino,
        @Schema(example = "2026-11-11T12:00:00Z") Instant fecha,
        @Schema(example = "duocconecta.contenido.interacciones") String topico) {

    /** Dónde queda el grupo. */
    public enum Destino {
        INICIO,
        FIN,
        FECHA
    }
}
