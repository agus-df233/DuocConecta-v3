// Enlaces directos a un proyecto: https://…/#proyecto-<id>. Con un # y no una ruta, porque el
// frontend se sirve desde S3 por el API Gateway y no hay servidor que reescriba rutas.
export const PREFIJO_ENLACE = '#proyecto-'

// Lo mismo para un prompt de la biblioteca: https://…/#prompt-<id>.
export const PREFIJO_PROMPT = '#prompt-'

// Dónde se guarda el enlace mientras la persona va y vuelve del login.
export const CLAVE_DESTINO = 'duocconecta.destino'

/** El enlace público de un proyecto. Quien lo abre igual tiene que iniciar sesión para verlo. */
export function enlaceDeProyecto(id) {
  return `${window.location.origin}/${PREFIJO_ENLACE}${id}`
}

/** El enlace de un prompt de la biblioteca. */
export function enlaceDePrompt(id) {
  return `${window.location.origin}/${PREFIJO_PROMPT}${id}`
}
