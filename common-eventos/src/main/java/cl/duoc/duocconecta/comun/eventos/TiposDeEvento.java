package cl.duoc.duocconecta.comun.eventos;

import java.util.Set;

/**
 * Catálogo de eventos: la única fuente de sus nombres.
 *
 * <p>Formato {@code categoria.hecho}, en minúsculas y sin tildes: el filtro de SNS compara texto
 * exacto y una tilde de más haría que el evento no le llegue a nadie.</p>
 */
public final class TiposDeEvento {

    public static final String PROYECTO_PUBLICADO = "proyecto.publicado";
    public static final String PROYECTO_ACTUALIZADO = "proyecto.actualizado";
    public static final String PROYECTO_ELIMINADO = "proyecto.eliminado";
    public static final String PROYECTO_COMENTADO = "proyecto.comentado";
    public static final String PROYECTO_COLABORADOR_AGREGADO = "proyecto.colaborador_agregado";
    public static final String PROYECTO_OCULTADO = "proyecto.ocultado";
    public static final String PROYECTO_RESTAURADO = "proyecto.restaurado";
    public static final String PROYECTO_COMENTARIO_ELIMINADO = "proyecto.comentario_eliminado";

    public static final String COLABORACION_SOLICITADA = "colaboracion.solicitada";
    public static final String COLABORACION_ACEPTADA = "colaboracion.aceptada";
    public static final String COLABORACION_RECHAZADA = "colaboracion.rechazada";
    public static final String COLABORACION_RETIRADA = "colaboracion.retirada";

    public static final String PROMPT_PUBLICADO = "prompt.publicado";
    public static final String PROMPT_ACTUALIZADO = "prompt.actualizado";
    public static final String PROMPT_ELIMINADO = "prompt.eliminado";
    /** Alguien hizo su copia de un prompt. {@code recursoId} es el original; {@code detalle}, la copia. */
    public static final String PROMPT_BIFURCADO = "prompt.bifurcado";
    /** {@code detalle} lleva las estrellas. */
    public static final String PROMPT_VALORADO = "prompt.valorado";
    public static final String PROMPT_OCULTADO = "prompt.ocultado";
    public static final String PROMPT_RESTAURADO = "prompt.restaurado";

    /** Todos los tipos conocidos: un consumidor descarta como no recuperable lo que no esté aquí. */
    public static final Set<String> CONOCIDOS = Set.of(
            PROYECTO_PUBLICADO, PROYECTO_ACTUALIZADO, PROYECTO_ELIMINADO, PROYECTO_COMENTADO,
            PROYECTO_COLABORADOR_AGREGADO, PROYECTO_OCULTADO, PROYECTO_RESTAURADO,
            PROYECTO_COMENTARIO_ELIMINADO,
            COLABORACION_SOLICITADA, COLABORACION_ACEPTADA, COLABORACION_RECHAZADA, COLABORACION_RETIRADA,
            PROMPT_PUBLICADO, PROMPT_ACTUALIZADO, PROMPT_ELIMINADO, PROMPT_BIFURCADO, PROMPT_VALORADO,
            PROMPT_OCULTADO, PROMPT_RESTAURADO);

    private TiposDeEvento() {
    }
}
