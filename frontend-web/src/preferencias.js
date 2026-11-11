// Preferencias de esta persona en este navegador: tamaño del texto, contraste, movimiento, avisos
// push (y cuánto duran) y proyectos escondidos. Son comodidades locales: si el navegador no deja
// guardar (ventana privada, almacenamiento bloqueado), la aplicación funciona igual con los
// valores por defecto.
import { useCallback, useState } from 'react'

const CLAVE = 'duocconecta.preferencias'

export const PREFERENCIAS_INICIALES = {
  texto: 0,            // 0 normal · 1 grande · 2 muy grande
  contraste: false,
  movimiento: false,   // true = reducir movimiento
  push: true,
  escondidos: [],      // ids de proyectos que esta persona sacó de su vitrina
  duracionAvisos: 0,   // 0 = 6 s · 1 = 12 s · 2 = hasta cerrarlos
}

/** Si el sistema pide menos movimiento, la aplicación parte así hasta que la persona elija otra cosa. */
function iniciales() {
  const sistema = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  return { ...PREFERENCIAS_INICIALES, movimiento: sistema }
}

function leer() {
  try {
    return { ...iniciales(), ...JSON.parse(window.localStorage.getItem(CLAVE) ?? '{}') }
  } catch {
    return iniciales()
  }
}

/** Las preferencias y una función para cambiar algunas. Cada cambio se guarda solo. */
export function usePreferencias() {
  const [preferencias, setPreferencias] = useState(leer)

  const cambiar = useCallback((cambios) => setPreferencias((actuales) => {
    const nuevas = { ...actuales, ...cambios }
    try {
      window.localStorage.setItem(CLAVE, JSON.stringify(nuevas))
    } catch {
      // Sin almacenamiento: el cambio vale solo mientras la pestaña siga abierta.
    }
    return nuevas
  }), [])

  return [preferencias, cambiar]
}
