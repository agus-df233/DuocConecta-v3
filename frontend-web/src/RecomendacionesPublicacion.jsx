// Las recomendaciones antes de publicar, con movimiento: un checklist que se marca solo mientras se
// escribe, consejos que aparecen al enfocar un campo, un carrusel la primera vez y una revisión con
// la vista previa de la tarjeta. Todo respeta "Reducir movimiento" (lo apaga el CSS).
import { useEffect, useState } from 'react'
import { CONSEJOS, PRIMERA_VEZ, marcarConsejosVistos } from './reglasPublicacion'

/** El checklist con la barra de avance. Lo pendiente lleva a su campo al tocarlo. */
export function ChecklistPublicacion({ reglas, alIrACampo }) {
  const cumplidas = reglas.filter((r) => r.cumple).length
  const total = reglas.length
  const proporcion = total ? cumplidas / total : 0
  const nivel = proporcion === 1 ? 'listo' : proporcion >= 0.5 ? 'bien' : 'empieza'
  return (
    <aside className={`recomendaciones ${nivel}`} aria-label="Recomendaciones antes de publicar">
      <div className="recomendaciones-cabecera">
        <strong>{proporcion === 1 ? '¡Listo para publicar!' : 'Para que destaque'}</strong>
        <span className="recomendaciones-cuenta" aria-live="polite">{cumplidas} de {total}</span>
      </div>
      <div className="riel-avance" aria-hidden="true">
        <span className="relleno-avance" style={{ '--avance': proporcion }} />
      </div>
      <ul>
        {reglas.map((r) => (
          <li key={r.id} className={r.cumple ? 'cumple' : ''}>
            <button type="button" className="regla" onClick={() => alIrACampo?.(r.campo)} disabled={r.cumple}>
              <span className="marca-regla" aria-hidden="true" />
              <span>{r.texto}</span>
              <span className="solo-lector">{r.cumple ? ' (listo)' : ' (pendiente)'}</span>
            </button>
          </li>
        ))}
      </ul>
    </aside>
  )
}

/** Burbuja con el consejo del campo enfocado. Se desliza al aparecer; Esc la cierra. */
export function ConsejoCampo({ campo, alCerrar }) {
  if (!campo || !CONSEJOS[campo]) return null
  return (
    <div className="consejo-campo" role="note" key={campo}>
      <span className="consejo-icono" aria-hidden="true">✦</span>
      <p>{CONSEJOS[campo]}</p>
      <button type="button" className="enlace" onClick={alCerrar} aria-label="Cerrar consejo">×</button>
    </div>
  )
}

/** Tres consejos en carrusel la primera vez que se abre el formulario. */
export function ConsejosPrimeraVez({ tipo, alTerminar }) {
  const pasos = PRIMERA_VEZ[tipo]
  const [paso, setPaso] = useState(0)
  const [noMostrar, setNoMostrar] = useState(true)
  const ultimo = paso === pasos.length - 1

  const terminar = () => {
    if (noMostrar) marcarConsejosVistos(tipo)
    alTerminar()
  }

  // Las flechas del teclado también avanzan.
  useEffect(() => {
    const alTeclear = (e) => {
      if (e.key === 'ArrowRight') setPaso((p) => Math.min(p + 1, pasos.length - 1))
      if (e.key === 'ArrowLeft') setPaso((p) => Math.max(p - 1, 0))
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [pasos.length])

  return (
    <div className="primera-vez">
      <div className="primera-vez-diapositiva" key={paso} aria-live="polite">
        <span className="primera-vez-numero" aria-hidden="true">{paso + 1}</span>
        <h3>{pasos[paso].titulo}</h3>
        <p>{pasos[paso].texto}</p>
      </div>
      <div className="primera-vez-puntos" aria-hidden="true">
        {pasos.map((_, i) => <span key={i} className={i === paso ? 'activo' : ''} />)}
      </div>
      <label className="casilla">
        <input type="checkbox" checked={noMostrar} onChange={(e) => setNoMostrar(e.target.checked)} />
        No volver a mostrar
      </label>
      <div className="acciones">
        {ultimo
          ? <button type="button" className="principal" onClick={terminar}>Empezar</button>
          : <button type="button" className="principal" onClick={() => setPaso(paso + 1)}>Siguiente</button>}
        <button type="button" className="secundario" onClick={terminar}>Saltar</button>
      </div>
    </div>
  )
}

/**
 * El paso antes de publicar: la tarjeta tal como saldrá, lo que falta y la decisión. La tarjeta
 * entra volteándose; lo pendiente se puede ir a arreglar.
 */
export function RevisionAntesDePublicar({ tarjeta, reglas, publicando, alMejorar, alPublicar }) {
  const pendientes = reglas.filter((r) => !r.cumple)
  return (
    <div className="revision-publicar">
      <p className="ayuda">Así se verá en la vitrina:</p>
      <div className="vista-previa-tarjeta" aria-label="Vista previa de la tarjeta">{tarjeta}</div>
      {pendientes.length > 0 ? (
        <div className="pendientes-revision">
          <strong>Te {pendientes.length === 1 ? 'falta 1 detalle' : `faltan ${pendientes.length} detalles`} para que destaque:</strong>
          <ul>{pendientes.map((r) => <li key={r.id}>{r.texto}</li>)}</ul>
        </div>
      ) : (
        <p className="todo-listo">Cumple todas las recomendaciones. ✓</p>
      )}
      <div className="acciones">
        <button type="button" className="principal" disabled={publicando} onClick={alPublicar}>
          {publicando ? 'Publicando…' : (pendientes.length ? 'Publicar igual' : 'Publicar')}
        </button>
        <button type="button" className="secundario" onClick={alMejorar}>Mejorar</button>
      </div>
    </div>
  )
}
