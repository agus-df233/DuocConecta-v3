package cl.duoc.duocconecta.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Todo lo de una persona para el administrador, en una sola llamada: su perfil, sus proyectos, sus
 * prompts y sus alertas de actividad. Es justo lo que hace un BFF: cuatro servicios, una respuesta.
 *
 * @param noDisponible partes que no se pudieron traer porque su servicio no respondió; el resto
 *                     llega igual
 */
@Schema(description = "Ficha de una persona para la administración")
public record FichaPersonaRespuesta(PersonaAdmin persona, List<ProyectoRespuesta> proyectos,
                                    List<PromptRespuesta> prompts, List<AlertaActividadRespuesta.DeAnalitica> alertas,
                                    List<String> noDisponible) {
}
