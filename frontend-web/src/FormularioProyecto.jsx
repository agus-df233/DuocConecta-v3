// Ventana para publicar o editar un proyecto: el mismo formulario en los dos casos.
// Valida antes de enviar, para no depender de un 400 del servidor. Con `abierto`, es el formulario
// del administrador para los proyectos abiertos: sin visibilidad y con una carrera obligatoria.
// Al lado, las recomendaciones se marcan solas mientras se escribe; al enfocar un campo aparece
// su consejo, y antes de publicar se ve la tarjeta tal como saldrá.
import { useState } from 'react'
import AdjuntosProyecto from './AdjuntosProyecto'
import { enCache } from './cache'
import { ESCUELAS, ESTADOS, HERRAMIENTAS_SUGERIDAS, SEDES } from './catalogo'
import {
  ChecklistPublicacion, ConsejoCampo, ConsejosPrimeraVez, RevisionAntesDePublicar,
} from './RecomendacionesPublicacion'
import { consejosVistos, recomendacionesProyecto } from './reglasPublicacion'
import TarjetaProyecto from './TarjetaProyecto'
import Ventana from './Ventana'

/** Un proyecto nuevo empieza público y buscando equipo, que es el caso más común. */
const VACIO = {
  nombre: '', resumen: '', descripcion: '', urlRepositorio: '', sede: SEDES[0],
  herramientas: [], estado: 'BUSCANDO_EQUIPO', visibilidad: 'PUBLICO', adjuntos: [], carrera: '',
}

export default function FormularioProyecto({ proyecto, abierto = false, alGuardar, alCerrar }) {
  const [datos, setDatos] = useState({ ...VACIO, ...(proyecto ?? {}) })
  const [errores, setErrores] = useState({})
  const [textos, setTextos] = useState({})   // texto crudo de los campos de lista
  const [guardando, setGuardando] = useState(false)

  const editando = Boolean(proyecto?.id)
  // 'consejos' (la primera vez) → 'formulario' → 'revision' (solo al publicar uno nuevo).
  const [etapa, setEtapa] = useState(() => (!editando && !abierto && !consejosVistos('proyecto') ? 'consejos' : 'formulario'))
  const [consejo, setConsejo] = useState(null)   // el campo enfocado
  const [aRevisar, setARevisar] = useState(null)

  // aria-invalid y aria-describedby: el lector de pantalla anuncia el error junto con el campo.
  const campo = (clave) => ({
    value: datos[clave] ?? '',
    onChange: (e) => {
      setDatos({ ...datos, [clave]: e.target.value })
      setErrores({ ...errores, [clave]: '' })
    },
    onFocus: () => setConsejo(clave),
    name: clave,
    className: errores[clave] ? 'malo' : '',
    'aria-invalid': Boolean(errores[clave]),
    'aria-describedby': errores[clave] ? `error-${clave}` : undefined,
  })

  /**
   * Las listas se escriben separadas por comas, y el texto se conserva tal cual mientras se
   * escribe. Partirlo en cada tecla borraba la coma recién escrita —el elemento vacío se
   * descartaba y el campo se volvía a serializar—, así que no se podía empezar la segunda.
   * La conversión a lista ocurre al salir del campo y al enviar.
   */
  const aLista = (texto) => texto.split(',').map((x) => x.trim()).filter(Boolean)

  const lista = (clave) => ({
    value: textos[clave] ?? (datos[clave] ?? []).join(', '),
    onChange: (e) => setTextos({ ...textos, [clave]: e.target.value }),
    onFocus: () => setConsejo(clave),
    name: clave,
    onBlur: () => {
      if (textos[clave] === undefined) return
      setDatos({ ...datos, [clave]: aLista(textos[clave]) })
      setTextos({ ...textos, [clave]: undefined })
    },
  })

  // Si se envía sin salir del campo, el texto en edición todavía no pasó a lista.
  const conPendientes = () => ({
    ...datos,
    ...Object.fromEntries(Object.entries(textos)
      .filter(([, texto]) => texto !== undefined)
      .map(([clave, texto]) => [clave, aLista(texto)])),
  })
  const reglas = recomendacionesProyecto(conPendientes())

  /** Lleva al campo de una recomendación pendiente. */
  const irACampo = (clave) => document.querySelector(`.modal [name="${clave}"]`)?.focus()

  const guardarAhora = async (datosFinales) => {
    setGuardando(true)
    try {
      await alGuardar(datosFinales)
    } finally {
      setGuardando(false)
    }
  }

  const enviar = async (evento) => {
    evento.preventDefault()
    const datosFinales = conPendientes()

    const fallos = {}
    // Un proyecto viejo puede traer el resumen en null: se trata como vacío y no rompe el formulario.
    const resumen = datos.resumen ?? ''
    if (!(datos.nombre ?? '').trim()) fallos.nombre = 'Ponle un nombre al proyecto.'
    if (!resumen.trim()) fallos.resumen = 'El resumen es lo único que se lee en la tarjeta.'
    if (resumen.length > 200) fallos.resumen = 'El resumen es de una línea: hasta 200 caracteres.'
    // En un proyecto abierto la carrera es la que se muestra y la que usan los filtros de la vitrina.
    if (abierto && !datos.carrera) fallos.carrera = 'Elige la carrera: es la que se muestra en la tarjeta.'
    if (Object.keys(fallos).length > 0) { setErrores(fallos); return }

    // Uno nuevo pasa primero por la revisión con la vista previa; editar guarda al tiro.
    if (!editando) { setARevisar(datosFinales); setEtapa('revision'); return }
    await guardarAhora(datosFinales)
  }

  if (etapa === 'consejos') {
    return (
      <Ventana titulo="Antes de publicar" alCerrar={alCerrar}>
        <ConsejosPrimeraVez tipo="proyecto" alTerminar={() => setEtapa('formulario')} />
      </Ventana>
    )
  }

  if (etapa === 'revision') {
    const perfil = enCache('mi-perfil')?.perfil ?? {}
    const vistaPrevia = {
      ...aRevisar, id: 'vista-previa', fechaCreacion: new Date().toISOString(), cantidadComentarios: 0,
      abierto, autor: { nombre: perfil.nombre, carrera: abierto ? aRevisar.carrera : perfil.carrera, sede: perfil.sede },
    }
    const nada = () => {}
    return (
      <Ventana titulo="Revisa antes de publicar" alCerrar={alCerrar} ancha>
        <RevisionAntesDePublicar
          reglas={recomendacionesProyecto(aRevisar)}
          publicando={guardando}
          alMejorar={() => setEtapa('formulario')}
          alPublicar={() => guardarAhora(aRevisar)}
          tarjeta={<TarjetaProyecto proyecto={vistaPrevia} esMio={false} alAbrir={nada} alColaborar={nada}
                                    alComentar={nada} alEditar={nada} alEsconder={nada} />}
        />
      </Ventana>
    )
  }

  return (
    <Ventana titulo={titulo(abierto, editando)} alCerrar={alCerrar} ancha>
      <div className="formulario-con-recomendaciones">
      <form onSubmit={enviar} onKeyDown={(e) => {
        // Con un consejo a la vista, Esc cierra el consejo y no la ventana.
        if (e.key === 'Escape' && consejo) { e.stopPropagation(); setConsejo(null) }
      }}>
        {abierto && (
          <p className="ayuda nota-abierto">
            Se muestra como <strong>Publicado por DuocConecta</strong>. Se puede comentar, pero no recibe
            solicitudes de colaboración.
          </p>
        )}
        <label>
          Nombre *
          <input maxLength={150} placeholder="Nombre del proyecto" {...campo('nombre')} />
          {errores.nombre && <span id="error-nombre" className="mensaje-campo" role="alert">{errores.nombre}</span>}
        </label>
        {consejo === 'nombre' && <ConsejoCampo campo="nombre" alCerrar={() => setConsejo(null)} />}

        <label>
          Resumen *
          <input maxLength={200} placeholder="Una línea que resuma el proyecto" {...campo('resumen')} />
          <span className="contador-campo" aria-hidden="true">{(datos.resumen ?? '').length}/200</span>
          {errores.resumen && <span id="error-resumen" className="mensaje-campo" role="alert">{errores.resumen}</span>}
        </label>
        {consejo === 'resumen' && <ConsejoCampo campo="resumen" alCerrar={() => setConsejo(null)} />}

        <label>
          Detalle
          <textarea
            rows={4}
            maxLength={2000}
            placeholder="Qué resuelve, en qué etapa está, qué ayuda necesitas"
            {...campo('descripcion')}
          />
          <span className="contador-campo" aria-hidden="true">{(datos.descripcion ?? '').length}/2000</span>
        </label>
        {consejo === 'descripcion' && <ConsejoCampo campo="descripcion" alCerrar={() => setConsejo(null)} />}

        {consejo === 'herramientas' && <ConsejoCampo campo="herramientas" alCerrar={() => setConsejo(null)} />}
        <div className="dos-columnas">
          <label>
            Herramientas (separadas por comas)
            <input
              list="herramientas-sugeridas"
              placeholder={HERRAMIENTAS_SUGERIDAS.slice(0, 3).join(', ')}
              {...lista('herramientas')}
            />
            <datalist id="herramientas-sugeridas">
              {HERRAMIENTAS_SUGERIDAS.map((h) => <option key={h} value={h} />)}
            </datalist>
          </label>

          <label>
            Estado *
            <select {...campo('estado')}>
              {Object.entries(ESTADOS).map(([valor, texto]) => (
                <option key={valor} value={valor}>{texto}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="dos-columnas">
          <label>
            Sede
            <select {...campo('sede')}>
              {SEDES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>

          {abierto ? (
            <label>
              Carrera *
              <select {...campo('carrera')}>
                <option value="">Elige una carrera</option>
                {ESCUELAS.map((e) => (
                  <optgroup key={e.nombre} label={e.nombre}>
                    {e.carreras.map((c) => <option key={c} value={c}>{c}</option>)}
                  </optgroup>
                ))}
              </select>
              {errores.carrera && <span id="error-carrera" className="mensaje-campo" role="alert">{errores.carrera}</span>}
            </label>
          ) : (
            <label>
              Visibilidad
              <select {...campo('visibilidad')}>
                <option value="PUBLICO">Visible en la vitrina</option>
                <option value="PRIVADO">Solo para mí</option>
              </select>
            </label>
          )}
        </div>

        <label>
          Enlace al repositorio o al trabajo
          <input type="url" placeholder="https://…" {...campo('urlRepositorio')} />
        </label>
        {consejo === 'urlRepositorio' && <ConsejoCampo campo="urlRepositorio" alCerrar={() => setConsejo(null)} />}

        <AdjuntosProyecto
          adjuntos={datos.adjuntos ?? []}
          alCambiar={(adjuntos) => setDatos({ ...datos, adjuntos })}
        />

        <div className="acciones">
          <button className="principal" type="submit" disabled={guardando}>
            {guardando ? 'Guardando…' : (editando ? 'Guardar cambios' : 'Revisar y publicar')}
          </button>
          <button className="secundario" type="button" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
      <ChecklistPublicacion reglas={reglas} alIrACampo={irACampo} />
      </div>
    </Ventana>
  )
}

/** El título de la ventana según el tipo de proyecto y si se está creando o editando. */
function titulo(abierto, editando) {
  if (abierto) return editando ? 'Editar proyecto abierto' : 'Publicar proyecto abierto'
  return editando ? 'Editar proyecto' : 'Publicar un proyecto'
}
