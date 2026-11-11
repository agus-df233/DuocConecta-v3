// Lo último que respondió el servidor, en memoria. Al volver a una vista se muestra al tiro lo
// que ya había y se pide lo nuevo por detrás ("stale-while-revalidate"): la navegación se siente
// instantánea y los datos igual quedan al día. Se pierde al recargar la página, a propósito: nada
// de lo que guarda sobrevive al cierre de sesión.
const memoria = new Map()

/** Lo guardado para una clave, o `undefined`. */
export const enCache = (clave) => memoria.get(clave)

/** Pide al servidor y guarda la respuesta. Devuelve la misma promesa que `pedir`. */
export async function conCache(clave, pedir) {
  const valor = await pedir()
  memoria.set(clave, valor)
  return valor
}

/** Olvida lo guardado bajo un prefijo (por ejemplo, al cerrar sesión). */
export function olvidar(prefijo = '') {
  for (const clave of memoria.keys()) if (clave.startsWith(prefijo)) memoria.delete(clave)
}
