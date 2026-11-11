package cl.duoc.duocconecta.mensajeria.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Cambios a la configuración de un tópico. Solo se tocan las claves nombradas.
 *
 * @param fijar   clave → valor nuevo
 * @param quitar  claves que vuelven al valor por defecto del broker
 */
public record ModificarConfiguracionDatos(
        @Schema(example = "{\"retention.ms\": \"172800000\"}") Map<String, String> fijar,
        @Schema(example = "[]") List<String> quitar) {

    /** Todas las claves que se tocan. */
    public List<String> claves() {
        var todas = new java.util.ArrayList<String>();
        if (fijar != null) {
            todas.addAll(fijar.keySet());
        }
        if (quitar != null) {
            todas.addAll(quitar);
        }
        return todas;
    }
}
