// Ventanas modales con lo que pide la accesibilidad: Esc cierra, el foco entra a la ventana, Tab
// no se escapa de ella y, al cerrar, el foco vuelve al botón que la abrió. "Atrás" también cierra,
// y la salida se anima (salvo con "Reducir movimiento").
import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { sinMovimiento, useCerrarConAtras } from './navegacion'

/** Lo que dura la animación de salida; coincide con la de estilos.css. */
const SALIDA_MS = 180

/**
 * Cierre con animación: marca la caja como "cerrando" y avisa al padre cuando la animación termina.
 * Devuelve `[cerrando, cerrar]`.
 */
export function useCierreAnimado(alCerrar) {
  const [cerrando, setCerrando] = useState(false)
  const avisar = useRef(alCerrar)
  avisar.current = alCerrar
  const cerrar = useCallback(() => {
    if (sinMovimiento()) { avisar.current(); return }
    setCerrando(true)
    setTimeout(() => avisar.current(), SALIDA_MS)
  }, [])
  return [cerrando, cerrar]
}

const ENFOCABLES = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), '
  + 'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

// Cuántas ventanas hay abiertas: el fondo se bloquea con la primera y se libera con la última.
let abiertas = 0

/**
 * Esc, foco atrapado y foco devuelto, para cualquier caja: una ventana o el panel lateral.
 * Con `enfocarCampo`, el foco va al primer campo; si no, a la caja misma, que no hace saltar el scroll.
 */
export function useAtraparFoco(alCerrar, { enfocarCampo = false } = {}) {
  useCerrarConAtras(alCerrar)
  const caja = useRef(null)
  const cerrar = useRef(alCerrar)
  cerrar.current = alCerrar

  useEffect(() => {
    const anterior = document.activeElement
    const elemento = caja.current
    const campo = enfocarCampo ? elemento?.querySelector('input:not([disabled]), textarea, select') : null
    ;(campo ?? elemento)?.focus()

    const alTeclear = (evento) => {
      if (evento.key === 'Escape') {
        evento.stopPropagation()
        cerrar.current()
        return
      }
      if (evento.key !== 'Tab') return
      const lista = [...elemento.querySelectorAll(ENFOCABLES)]
      if (lista.length === 0) return
      const primero = lista[0]
      const ultimo = lista[lista.length - 1]
      if (evento.shiftKey && document.activeElement === primero) {
        evento.preventDefault()
        ultimo.focus()
      } else if (!evento.shiftKey && document.activeElement === ultimo) {
        evento.preventDefault()
        primero.focus()
      }
    }

    elemento?.addEventListener('keydown', alTeclear)
    abiertas += 1
    document.body.classList.add('sin-scroll')
    return () => {
      elemento?.removeEventListener('keydown', alTeclear)
      abiertas -= 1
      if (abiertas === 0) document.body.classList.remove('sin-scroll')
      anterior?.focus?.()
    }
  }, [enfocarCampo])

  return caja
}

/** Ventana centrada con su título y el botón de cerrar. El contenido va en `children`. */
export default function Ventana({ titulo, alCerrar, children, ancha = false, enfocarCampo = true }) {
  const id = useId()
  const [cerrando, cerrar] = useCierreAnimado(alCerrar)
  const caja = useAtraparFoco(cerrar, { enfocarCampo })

  return (
    <>
      <div className={cerrando ? 'modal-fondo cerrando' : 'modal-fondo'} onClick={cerrar} />
      <div className="modal-caja">
        <div
          ref={caja}
          className={`${ancha ? 'modal ancha' : 'modal'}${cerrando ? ' cerrando' : ''}`}
          role="dialog"
          aria-modal="true"
          aria-labelledby={id}
          tabIndex={-1}
        >
          <div className="modal-cabecera">
            <h2 id={id}>{titulo}</h2>
            <button type="button" className="cerrar" onClick={cerrar} aria-label="Cerrar">×</button>
          </div>
          {children}
        </div>
      </div>
    </>
  )
}
