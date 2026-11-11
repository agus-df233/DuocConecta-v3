// Navegación con el botón Atrás. No hay router: la vista vive en el hash (#/vitrina, #/prompts,
// #/perfil, #/admin/kafka) y cada cambio queda en el historial del navegador. Con hash y no con
// rutas porque el frontend se sirve desde S3 por el API Gateway y no hay servidor que reescriba.
//
// Lo que no se reconoce se ignora: así no se pisa la vuelta del login (MSAL o Cognito).
import { useEffect, useRef } from 'react'
import { PREFIJO_ENLACE, PREFIJO_PROMPT } from './enlaces'

/** Nombre de la vista en el código → nombre en la dirección. */
const EN_LA_DIRECCION = { vitrina: 'vitrina', biblioteca: 'prompts', perfil: 'perfil', admin: 'admin' }
const DESDE_LA_DIRECCION = Object.fromEntries(Object.entries(EN_LA_DIRECCION).map(([v, d]) => [d, v]))

/**
 * Lo que dice la dirección actual: `{ vista, seccion, foco }`, o `null` si el hash no es nuestro.
 * Los enlaces compartidos de siempre (#proyecto-<id>, #prompt-<id>) siguen funcionando.
 */
export function leerRuta(hash = window.location.hash) {
  if (hash.startsWith(PREFIJO_ENLACE)) {
    return { vista: 'vitrina', seccion: null, foco: { proyectoId: hash.slice(PREFIJO_ENLACE.length) } }
  }
  if (hash.startsWith(PREFIJO_PROMPT)) {
    return { vista: 'biblioteca', seccion: null, foco: { promptId: hash.slice(PREFIJO_PROMPT.length) } }
  }
  const partes = hash.match(/^#\/([a-z]+)(?:\/([a-z-]+))?$/)
  const vista = partes && DESDE_LA_DIRECCION[partes[1]]
  return vista ? { vista, seccion: partes[2] ?? null, foco: null } : null
}

/** La dirección de una vista. */
export function rutaDe(vista, seccion = null) {
  return `#/${EN_LA_DIRECCION[vista] ?? 'vitrina'}${seccion ? `/${seccion}` : ''}`
}

/** Anota una vista en el historial. `reemplazar` no suma una entrada (para la primera carga). */
export function escribirRuta(vista, seccion = null, { reemplazar = false } = {}) {
  const destino = rutaDe(vista, seccion)
  if (window.location.hash === destino && !reemplazar) return
  const estado = { vista, seccion }
  if (reemplazar) window.history.replaceState(estado, '', destino)
  else window.history.pushState(estado, '', destino)
}

/**
 * Hace que "Atrás" cierre una ventana o un panel en vez de salir de la vista. Al abrirse suma una
 * entrada al historial; si se cierra con su botón, la quita. Sirve anidado: cada capa tiene su marca.
 */
export function useCerrarConAtras(alCerrar) {
  const cerrar = useRef(alCerrar)
  cerrar.current = alCerrar

  useEffect(() => {
    const marca = Math.random().toString(36).slice(2)
    window.history.pushState({ ...(window.history.state ?? {}), capa: marca }, '')
    let porAtras = false

    const alVolver = (evento) => {
      if (evento.state?.capa === marca) return
      porAtras = true
      cerrar.current()
    }
    window.addEventListener('popstate', alVolver)

    return () => {
      window.removeEventListener('popstate', alVolver)
      // Se espera un instante: en desarrollo React monta dos veces, y la segunda capa ya está arriba.
      setTimeout(() => {
        if (!porAtras && window.history.state?.capa === marca) window.history.back()
      }, 0)
    }
  }, [])
}

/** Si hay que ahorrarse el movimiento: la preferencia de la persona o la del sistema. */
export function sinMovimiento() {
  return document.documentElement.dataset.movimiento === 'reducido'
    || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
}
