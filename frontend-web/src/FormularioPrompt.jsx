// Ventana para publicar, editar o terminar una copia de un prompt: el mismo formulario. Valida
// antes de enviar, para no depender de un 400 del servidor.
import { useState } from 'react'
import { CATEGORIAS_PROMPT, ESCUELAS, HERRAMIENTAS_IA } from './catalogo'
import Ventana from './Ventana'

/** Un prompt nuevo empieza público: la biblioteca es para compartir. */
const VACIO = {
  titulo: '', proposito: '', contenido: '', categoria: 'ESTUDIO', carrera: '', herramienta: '', visibilidad: 'PUBLICO',
}

export default function FormularioPrompt({ prompt, esCopia = false, alGuardar, alCerrar }) {
  const [datos, setDatos] = useState({ ...VACIO, ...(prompt ?? {}), carrera: prompt?.carrera ?? '', herramienta: prompt?.herramienta ?? '' })
  const [errores, setErrores] = useState({})
  const [guardando, setGuardando] = useState(false)
  const editando = Boolean(prompt?.id)

  const campo = (clave) => ({
    value: datos[clave] ?? '',
    onChange: (e) => {
      setDatos({ ...datos, [clave]: e.target.value })
      setErrores({ ...errores, [clave]: '' })
    },
    className: errores[clave] ? 'malo' : '',
    'aria-invalid': Boolean(errores[clave]),
    'aria-describedby': errores[clave] ? `error-prompt-${clave}` : undefined,
  })

  const error = (clave) => errores[clave]
    && <span id={`error-prompt-${clave}`} className="mensaje-campo" role="alert">{errores[clave]}</span>

  const enviar = async (evento) => {
    evento.preventDefault()
    const fallos = {}
    if (!datos.titulo.trim()) fallos.titulo = 'Ponle un título.'
    if (!datos.proposito.trim()) fallos.proposito = 'Cuenta para qué sirve: es lo que se lee en la tarjeta.'
    if (!datos.contenido.trim()) fallos.contenido = 'Falta el texto del prompt.'
    if (Object.keys(fallos).length > 0) { setErrores(fallos); return }
    setGuardando(true)
    try {
      await alGuardar(datos)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <Ventana titulo={esCopia ? 'Tu copia del prompt' : (editando ? 'Editar prompt' : 'Publicar un prompt')} alCerrar={alCerrar} ancha>
      <form onSubmit={enviar}>
        {esCopia && (
          <p className="ayuda nota-abierto">
            Es tu copia y nace como borrador: ajústala y, cuando esté lista, déjala pública. El original
            queda enlazado.
          </p>
        )}
        <label>
          Título *
          <input maxLength={150} placeholder="Qué hace el prompt" {...campo('titulo')} />
          {error('titulo')}
        </label>

        <label>
          Para qué sirve *
          <input maxLength={500} placeholder="En una línea: qué problema resuelve" {...campo('proposito')} />
          <span className="contador-campo" aria-hidden="true">{datos.proposito.length}/500</span>
          {error('proposito')}
        </label>

        <label>
          El prompt *
          <textarea rows={8} maxLength={8000} className={`prompt-texto ${errores.contenido ? 'malo' : ''}`}
                    placeholder="El texto tal cual se copia. Marca con [corchetes] lo que hay que completar."
                    value={datos.contenido}
                    aria-invalid={Boolean(errores.contenido)}
                    aria-describedby={errores.contenido ? 'error-prompt-contenido' : undefined}
                    onChange={(e) => { setDatos({ ...datos, contenido: e.target.value }); setErrores({ ...errores, contenido: '' }) }} />
          <span className="contador-campo" aria-hidden="true">{datos.contenido.length}/8000</span>
          {error('contenido')}
        </label>

        <div className="dos-columnas">
          <label>
            Categoría *
            <select {...campo('categoria')}>
              {Object.entries(CATEGORIAS_PROMPT).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
            </select>
          </label>
          <label>
            Herramienta de IA
            <input list="herramientas-ia" maxLength={80} placeholder="ChatGPT, Claude, Gemini…" {...campo('herramienta')} />
            <datalist id="herramientas-ia">
              {HERRAMIENTAS_IA.map((h) => <option key={h} value={h} />)}
            </datalist>
          </label>
        </div>

        <div className="dos-columnas">
          <label>
            Carrera
            <select {...campo('carrera')}>
              <option value="">Para cualquier carrera</option>
              {ESCUELAS.map((e) => (
                <optgroup key={e.nombre} label={e.nombre}>
                  {e.carreras.map((c) => <option key={c} value={c}>{c}</option>)}
                </optgroup>
              ))}
            </select>
          </label>
          <label>
            Visibilidad
            <select {...campo('visibilidad')}>
              <option value="PUBLICO">Visible en la biblioteca</option>
              <option value="PRIVADO">Borrador: solo para mí</option>
            </select>
          </label>
        </div>

        <div className="acciones">
          <button className="principal" type="submit" disabled={guardando}>
            {guardando ? 'Guardando…' : (editando ? 'Guardar cambios' : 'Publicar')}
          </button>
          <button className="secundario" type="button" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}
