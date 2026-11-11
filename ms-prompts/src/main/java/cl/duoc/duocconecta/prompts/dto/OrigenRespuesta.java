package cl.duoc.duocconecta.prompts.dto;

import java.util.UUID;

/**
 * El prompt del que salió una copia, si quien mira todavía puede verlo.
 *
 * @param id      identificador del original
 * @param titulo  su título
 * @param autorId {@code oid} de quien lo publicó (el BFF le pone nombre)
 */
public record OrigenRespuesta(UUID id, String titulo, String autorId) {
}
