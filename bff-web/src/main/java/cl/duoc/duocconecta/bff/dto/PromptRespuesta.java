package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Un prompt de la biblioteca con su autor ya resuelto, y el del original si es una copia.
 * ms-prompts solo conoce los {@code oid}; el BFF les pone nombre.
 */
@Schema(description = "Prompt de la biblioteca con el autor resuelto por el BFF")
public record PromptRespuesta(UUID id, String titulo, String proposito, String contenido, String categoria,
                              String carrera, String herramienta, String autorId, Autor autor, String visibilidad,
                              Origen origen, int bifurcaciones, double promedio, int valoraciones,
                              Integer miValoracion, boolean oculto, String motivoOcultamiento, Instant creadoEn,
                              Instant actualizadoEn) {

    /**
     * El prompt del que salió la copia.
     *
     * @param autor nombre de quien publicó el original; {@code autorId} solo viene de ms-prompts
     */
    public record Origen(UUID id, String titulo, String autorId, Autor autor) {
    }

    /** Copia con los autores resueltos. */
    public PromptRespuesta con(Autor autorResuelto, Autor autorOrigen) {
        Origen conNombre = origen == null ? null
                : new Origen(origen.id(), origen.titulo(), origen.autorId(), autorOrigen);
        return new PromptRespuesta(id, titulo, proposito, contenido, categoria, carrera, herramienta, autorId,
                autorResuelto, visibilidad, conNombre, bifurcaciones, promedio, valoraciones, miValoracion, oculto,
                motivoOcultamiento, creadoEn, actualizadoEn);
    }
}
