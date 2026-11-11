package cl.duoc.duocconecta.prompts.dto;

import cl.duoc.duocconecta.prompts.domain.CategoriaPrompt;
import cl.duoc.duocconecta.prompts.domain.Prompt;
import cl.duoc.duocconecta.prompts.domain.Visibilidad;
import java.time.Instant;
import java.util.UUID;

/**
 * Un prompt, como lo ve quien lo pidió. Solo identificadores de personas: el BFF agrega nombres.
 *
 * @param origen        el original, si es una copia y sigue visible; nulo si no
 * @param promedio      estrellas promedio, con un decimal
 * @param valoraciones  cuántas personas lo valoraron
 * @param miValoracion  las estrellas que le dio quien mira; nulo si no lo valoró (solo en el detalle)
 * @param motivoOcultamiento solo lo ve el autor: por qué lo ocultó la moderación
 */
public record PromptRespuesta(UUID id, String titulo, String proposito, String contenido, CategoriaPrompt categoria,
                              String carrera, String herramienta, String autorId, Visibilidad visibilidad,
                              OrigenRespuesta origen, int bifurcaciones, double promedio, int valoraciones,
                              Integer miValoracion, boolean oculto, String motivoOcultamiento, Instant creadoEn,
                              Instant actualizadoEn) {

    /** Desde la entidad, para quien mira. El original solo se muestra si esa persona puede verlo. */
    public static PromptRespuesta desde(Prompt p, String quienMira, Integer miValoracion) {
        Prompt original = p.getOrigen();
        OrigenRespuesta origen = original != null && original.esVisiblePara(quienMira)
                ? new OrigenRespuesta(original.getId(), original.getTitulo(), original.getAutorId()) : null;
        boolean esAutor = p.getAutorId().equals(quienMira);
        return new PromptRespuesta(p.getId(), p.getTitulo(), p.getProposito(), p.getContenido(), p.getCategoria(),
                p.getCarrera(), p.getHerramienta(), p.getAutorId(), p.getVisibilidad(), origen, p.getBifurcaciones(),
                p.promedio(), p.getCantidadValoraciones(), miValoracion, p.isOculto(),
                esAutor ? p.getMotivoOcultamiento() : null, p.getCreadoEn(), p.getActualizadoEn());
    }

    /** Para la moderación: con el motivo, aunque quien mira no sea el autor. */
    public static PromptRespuesta paraModerar(Prompt p) {
        return desde(p, p.getAutorId(), null);
    }
}
