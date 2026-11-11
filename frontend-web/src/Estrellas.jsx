// Valoración con estrellas. Es un grupo de botones de radio de verdad, así el lector de pantalla
// anuncia "3 de 5" y se puede elegir con las flechas; las estrellas son solo la apariencia.
import { useId } from 'react'

/** Para elegir: llama a `alElegir` con 1 a 5. */
export function ElegirEstrellas({ valor, alElegir, deshabilitado = false }) {
  const nombre = useId()
  return (
    <fieldset className="estrellas" disabled={deshabilitado}>
      <legend>Tu valoración</legend>
      {[1, 2, 3, 4, 5].map((n) => (
        <label key={n} className={valor >= n ? 'llena' : ''}>
          <input type="radio" name={nombre} value={n} checked={valor === n} onChange={() => alElegir(n)} />
          <span aria-hidden="true">★</span>
          <span className="solo-lector">{n === 1 ? '1 estrella' : `${n} estrellas`}</span>
        </label>
      ))}
    </fieldset>
  )
}

/** Para mostrar el promedio: "★ 4,5 · 3 valoraciones". */
export function PromedioEstrellas({ promedio, valoraciones }) {
  if (!valoraciones) return <span className="promedio-estrellas sin-valorar">Sin valoraciones</span>
  const texto = promedio.toLocaleString('es-CL', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
  return (
    <span className="promedio-estrellas" aria-label={`${texto} de 5 estrellas, ${valoraciones} valoraciones`}>
      <span aria-hidden="true">★</span> {texto} · {valoraciones}
    </span>
  )
}
