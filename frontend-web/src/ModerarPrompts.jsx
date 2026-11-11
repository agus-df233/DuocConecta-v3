// Moderación de la biblioteca de prompts (solo ADMINISTRADOR): cifras, ocultar con motivo,
// mostrar y borrar. Quien protege de verdad es el 403 de cada ruta /admin.
import { useCallback, useEffect, useState } from 'react'
import {
  eliminarPromptModerado, estadisticasPrompts, mostrarPrompt, ocultarPrompt, promptsParaModerar,
} from './api'
import Aviso, { useAviso } from './Aviso'
import { CATEGORIAS_PROMPT, fechaCorta } from './catalogo'
import { Barras, Cifra } from './Cifras'
import Confirmar from './Confirmar'
import Ventana from './Ventana'
import { EsqueletoLista } from './Esqueleto'

export default function ModerarPrompts() {
  const [filtro, setFiltro] = useState('')
  const [prompts, setPrompts] = useState(null)
  const [cifras, setCifras] = useState(null)
  const [error, setError] = useState(null)
  const [ocultando, setOcultando] = useState(null)
  const [eliminando, setEliminando] = useState(null)
  const { texto: mensaje, avisar } = useAviso()

  const cargar = useCallback(() => {
    promptsParaModerar(filtro).then(setPrompts).catch((e) => setError(e.message))
    estadisticasPrompts().then(setCifras).catch(() => setCifras(null))
  }, [filtro])
  useEffect(() => { cargar() }, [cargar])

  const actuar = async (accion, texto) => {
    setError(null)
    try {
      await accion()
      avisar(texto)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      {cifras && (
        <>
          <div className="cifras">
            <Cifra valor={cifras.total} texto="Prompts" detalle={`${cifras.publicos} en la biblioteca`} />
            <Cifra valor={cifras.ocultos} texto="Ocultos por moderación" alerta={cifras.ocultos > 0} />
            <Cifra valor={cifras.valoraciones} texto="Valoraciones" />
          </div>
          <div className="grilla">
            <Barras titulo="Prompts por categoría" conteos={cifras.porCategoria} etiquetas={CATEGORIAS_PROMPT} />
            <Barras titulo="Prompts por carrera" conteos={cifras.porCarrera} />
          </div>
        </>
      )}

      <div className="filtros filtros-admin">
        <select aria-label="Estado de moderación" value={filtro} onChange={(e) => setFiltro(e.target.value)}>
          <option value="">Todos</option>
          <option value="false">En la biblioteca</option>
          <option value="true">Ocultos</option>
        </select>
      </div>

      {error && <div className="error" role="alert">{error}</div>}
      {!prompts ? <EsqueletoLista texto="Cargando prompts" /> : prompts.length === 0 ? (
        <div className="vacio">No hay prompts con ese filtro.</div>
      ) : (
        <div className="tabla-scroll">
          <table className="tabla-admin">
            <thead><tr><th>Prompt</th><th>Categoría</th><th>Visibilidad</th><th>Valoración</th><th>Publicado</th><th /></tr></thead>
            <tbody>
              {prompts.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.titulo}</strong>
                    {p.oculto && <span className="insignia moderado">Oculto: {p.motivoOcultamiento}</span>}
                  </td>
                  <td>{CATEGORIAS_PROMPT[p.categoria] ?? p.categoria}</td>
                  <td>{p.visibilidad === 'PUBLICO' ? 'Público' : 'Borrador'}</td>
                  <td>{p.valoraciones ? `${p.promedio} (${p.valoraciones})` : '—'}</td>
                  <td>{fechaCorta(p.creadoEn)}</td>
                  <td className="acciones-fila">
                    {p.oculto
                      ? <button className="secundario" onClick={() => actuar(() => mostrarPrompt(p.id), 'Prompt de vuelta en la biblioteca.')}>Mostrar</button>
                      : <button className="secundario" onClick={() => setOcultando(p)}>Ocultar</button>}
                    <button className="secundario peligro" onClick={() => setEliminando(p)}>Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {ocultando && (
        <OcultarPrompt prompt={ocultando} alCerrar={() => setOcultando(null)}
                       alConfirmar={(motivo) => { setOcultando(null); actuar(() => ocultarPrompt(ocultando.id, motivo), 'Prompt oculto.') }} />
      )}
      {eliminando && (
        <Confirmar titulo="Eliminar prompt" textoAccion="Eliminar" alCerrar={() => setEliminando(null)}
                   alConfirmar={() => { setEliminando(null); actuar(() => eliminarPromptModerado(eliminando.id), 'Prompt eliminado.') }}>
          Se borra <strong>{eliminando.titulo}</strong>. Las copias que otras personas hicieron siguen.
        </Confirmar>
      )}
      <Aviso texto={mensaje} />
    </>
  )
}

/** Pide el motivo antes de ocultar: su autor lo ve en el detalle. */
function OcultarPrompt({ prompt, alCerrar, alConfirmar }) {
  const [motivo, setMotivo] = useState('')
  const valido = motivo.trim().length > 0 && motivo.length <= 500
  return (
    <Ventana titulo={`Ocultar "${prompt.titulo}"`} alCerrar={alCerrar}>
      <form onSubmit={(e) => { e.preventDefault(); if (valido) alConfirmar(motivo.trim()) }}>
        <label>
          Motivo (quien lo publicó lo verá)
          <textarea value={motivo} maxLength={500} rows={4} onChange={(e) => setMotivo(e.target.value)} />
        </label>
        <p className="ayuda">{motivo.length}/500</p>
        <div className="acciones">
          <button type="submit" className="principal" disabled={!valido}>Ocultar prompt</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}
