// Vista de administración, en tres grupos: Comunidad (cifras y personas), Contenido (moderación de
// proyectos, comentarios y prompts) y Plataforma (lo que llega por Kafka y la mensajería de la EP2).
// Solo aparece para ADMINISTRADOR, pero quien protege de verdad es el backend: cada ruta /admin
// responde 403 a cualquier otra cuenta.
import { useCallback, useEffect, useState } from 'react'
import {
  comentariosParaModerar,
  editarProyectoAbierto,
  eliminarComentarioModerado,
  eliminarProyectoModerado,
  mostrarProyecto,
  ocultarProyecto,
  proyectosParaModerar,
  publicarProyectoAbierto,
  resumenAdministracion,
} from './api'
import { Actividad, Alertas } from './AdministracionStreaming'
import AdminKafka from './AdminKafka'
import AdminMensajeria from './AdminMensajeria'
import AdminPersonas from './AdminPersonas'
import { Barras, Cifra } from './Cifras'
import ModerarPrompts from './ModerarPrompts'
import Aviso, { useAviso } from './Aviso'
import { ESTADOS, fechaCorta, fechaLarga } from './catalogo'
import Confirmar from './Confirmar'
import FormularioProyecto from './FormularioProyecto'
import Ventana from './Ventana'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'

/** Las secciones, agrupadas. El id es el que va en la dirección: #/admin/<id>. */
const GRUPOS = [
  { titulo: 'Comunidad', secciones: [
    { id: 'resumen', texto: 'Resumen', Vista: Resumen },
    { id: 'personas', texto: 'Personas', Vista: AdminPersonas },
  ] },
  { titulo: 'Contenido', secciones: [
    { id: 'proyectos', texto: 'Proyectos', Vista: ModerarProyectos },
    { id: 'comentarios', texto: 'Comentarios', Vista: ModerarComentarios },
    { id: 'prompts', texto: 'Prompts', Vista: ModerarPrompts },
  ] },
  { titulo: 'Plataforma', secciones: [
    { id: 'alertas', texto: 'Alertas', Vista: Alertas },
    { id: 'actividad', texto: 'Actividad', Vista: Actividad },
    { id: 'kafka', texto: 'Kafka', Vista: AdminKafka },
    { id: 'mensajeria', texto: 'Mensajería', Vista: AdminMensajeria },
  ] },
]
const SECCIONES = GRUPOS.flatMap((g) => g.secciones)

const VISIBILIDADES = { PUBLICO: 'Público', COMPARTIDO: 'Compartido', PRIVADO: 'Privado' }

/**
 * La sección viene de la dirección (#/admin/kafka), así "Atrás" vuelve a la pestaña anterior y un
 * enlace lleva directo a una sección.
 */
export default function Administracion({ seccion, alCambiarSeccion }) {
  const actual = SECCIONES.find((x) => x.id === seccion) ?? SECCIONES[0]
  const { Vista } = actual

  return (
    <section>
      <div className="titulo-pagina">
        <div>
          <h1>Administración</h1>
          <p>Comunidad, moderación del contenido y la plataforma de mensajería.</p>
        </div>
      </div>

      <div className="admin-layout">
        {/* En el computador, una barra lateral con los grupos; en el celular, un desplegable. */}
        <nav className="admin-menu" aria-label="Secciones de administración">
          {GRUPOS.map((g) => (
            <div key={g.titulo} className="admin-grupo">
              <p className="rotulo">{g.titulo}</p>
              {g.secciones.map((x) => (
                <button key={x.id} type="button" className={x.id === actual.id ? 'activa' : ''}
                        aria-current={x.id === actual.id ? 'page' : undefined} onClick={() => alCambiarSeccion(x.id)}>
                  {x.texto}
                </button>
              ))}
            </div>
          ))}
        </nav>
        <label className="admin-menu-movil">
          <span className="solo-lector">Sección</span>
          <select value={actual.id} onChange={(e) => alCambiarSeccion(e.target.value)}>
            {GRUPOS.map((g) => (
              <optgroup key={g.titulo} label={g.titulo}>
                {g.secciones.map((x) => <option key={x.id} value={x.id}>{x.texto}</option>)}
              </optgroup>
            ))}
          </select>
        </label>
        <div className="admin-contenido" key={actual.id}>
          <h2 className="admin-titulo-seccion">{actual.texto}</h2>
          <Vista />
        </div>
      </div>
    </section>
  )
}

/** Tarjetas con los totales y barras por categoría. */
function Resumen() {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    resumenAdministracion().then(setDatos).catch((e) => setError(e.message))
  }, [])

  if (error) return <div className="error">{error}</div>
  if (!datos) return <EsqueletoLista filas={8} texto="Calculando las cifras" />

  const { comunidad, vitrina } = datos
  return (
    <>
      <div className="cifras">
        <Cifra valor={comunidad.total} texto="Perfiles" detalle={`${comunidad.visibles} visibles`} />
        <Cifra valor={vitrina.total} texto="Proyectos" detalle={`${vitrina.enVitrina} en la vitrina`} />
        <Cifra valor={vitrina.ocultosPorModeracion} texto="Ocultos por moderación" alerta={vitrina.ocultosPorModeracion > 0} />
        <Cifra valor={vitrina.publicadosUltimos30Dias} texto="Publicados en 30 días" />
        <Cifra valor={vitrina.comentarios} texto="Comentarios" />
      </div>

      <div className="grilla">
        <Barras titulo="Perfiles por rol" conteos={comunidad.porRol} />
        <Barras titulo="Perfiles por carrera" conteos={comunidad.porCarrera} />
        <Barras titulo="Proyectos por carrera" conteos={vitrina.porCarrera}
                ayuda="Según la carrera de quien lo publicó." />
        <Barras titulo="Proyectos por estado" conteos={vitrina.porEstado} etiquetas={ESTADOS} />
        <Barras titulo="Proyectos por visibilidad" conteos={vitrina.porVisibilidad} etiquetas={VISIBILIDADES} />
        <Barras titulo="Perfiles por sede" conteos={comunidad.porSede} />
      </div>
    </>
  )
}

/**
 * Tabla de todos los proyectos con las acciones de moderación. Desde aquí también se publican y
 * editan los proyectos abiertos, los que muestra la vitrina como de DuocConecta.
 */
function ModerarProyectos() {
  const [filtros, setFiltros] = useState({ oculto: '', visibilidad: '' })
  const [proyectos, setProyectos] = useState(null)
  const [error, setError] = useState(null)
  const { texto: mensaje, avisar } = useAviso()
  const [ocultando, setOcultando] = useState(null)
  const [eliminando, setEliminando] = useState(null)
  const [editandoAbierto, setEditandoAbierto] = useState(null)   // null cerrado · {} nuevo · proyecto si edita

  const cargar = useCallback(() => {
    setError(null)
    proyectosParaModerar(filtros).then(setProyectos).catch((e) => setError(e.message))
  }, [filtros])

  // El formulario se cierra solo si se guardó: con un error, sigue abierto para corregir.
  const guardarAbierto = async (datos) => {
    try {
      if (editandoAbierto?.id) await editarProyectoAbierto(editandoAbierto.id, datos)
      else await publicarProyectoAbierto(datos)
      avisar(editandoAbierto?.id ? 'Proyecto abierto actualizado.' : 'Proyecto abierto publicado en la vitrina.')
      setEditandoAbierto(null)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { cargar() }, [cargar])

  // Cada acción recarga la tabla: así se ve el estado real que quedó en el servidor.
  const actuar = async (accion, textoListo) => {
    setError(null)
    try {
      await accion()
      avisar(textoListo)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const eliminar = async () => {
    const p = eliminando
    setEliminando(null)
    await actuar(() => eliminarProyectoModerado(p.id), 'Proyecto eliminado. La dueña recibe un aviso.')
  }

  return (
    <>
      <div className="filtros filtros-admin">
        <select value={filtros.oculto} onChange={(e) => setFiltros({ ...filtros, oculto: e.target.value })}
                aria-label="Filtrar por moderación">
          <option value="">Todos</option>
          <option value="false">En la vitrina o de su dueña</option>
          <option value="true">Ocultos por moderación</option>
        </select>
        <select value={filtros.visibilidad} onChange={(e) => setFiltros({ ...filtros, visibilidad: e.target.value })}
                aria-label="Filtrar por visibilidad">
          <option value="">Cualquier visibilidad</option>
          {Object.entries(VISIBILIDADES).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
        </select>
        {proyectos && <span className="resumen"><strong>{proyectos.length}</strong> proyectos</span>}
        <button type="button" className="principal" onClick={() => setEditandoAbierto({})}>
          Publicar proyecto abierto
        </button>
      </div>

      {error && <div className="error" role="alert">{error}</div>}
      {!proyectos && !error && <EsqueletoLista texto="Cargando proyectos" />}

      {proyectos && (
        <div className="tarjeta tabla-scroll">
          <TablaAdmin>
            <thead>
              <tr><th>Proyecto</th><th>Estado</th><th>Visibilidad</th><th>Publicado</th><th>Moderación</th><th /></tr>
            </thead>
            <tbody>
              {proyectos.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.nombre}</strong>
                    <span className="ayuda">{p.sede || 'Sin sede'} · {p.cantidadComentarios} comentarios</span>
                  </td>
                  <td><span className={`insignia ${p.estado}`}>{ESTADOS[p.estado] ?? p.estado}</span></td>
                  <td>{VISIBILIDADES[p.visibilidad] ?? p.visibilidad}</td>
                  <td>{fechaCorta(p.fechaCreacion)}</td>
                  <td>
                    {p.oculto && <span className="insignia moderado" title={p.motivoOcultamiento}>Oculto: {p.motivoOcultamiento}</span>}
                    {p.abierto && <span className="insignia abierto" title={p.carrera}>Abierto · {p.carrera}</span>}
                    {!p.oculto && !p.abierto && <span className="ayuda">Sin acciones</span>}
                  </td>
                  <td className="acciones-fila">
                    {p.abierto && <button className="secundario" onClick={() => setEditandoAbierto(p)}>Editar</button>}
                    {p.oculto
                      ? <button className="secundario" onClick={() => actuar(() => mostrarProyecto(p.id), 'Proyecto de vuelta en la vitrina.')}>Mostrar</button>
                      : <button className="secundario" onClick={() => setOcultando(p)}>Ocultar</button>}
                    <button className="secundario peligro" onClick={() => setEliminando(p)}>Eliminar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </TablaAdmin>
          {proyectos.length === 0 && <p className="ayuda">No hay proyectos con estos filtros.</p>}
        </div>
      )}

      {ocultando && (
        <OcultarProyecto
          proyecto={ocultando}
          alCerrar={() => setOcultando(null)}
          alConfirmar={(motivo) => {
            setOcultando(null)
            actuar(() => ocultarProyecto(ocultando.id, motivo), 'Proyecto oculto. La dueña recibe un aviso con el motivo.')
          }}
        />
      )}

      {editandoAbierto && (
        <FormularioProyecto
          abierto
          proyecto={editandoAbierto.id ? editandoAbierto : null}
          alGuardar={guardarAbierto}
          alCerrar={() => setEditandoAbierto(null)}
        />
      )}

      {eliminando && (
        <Confirmar titulo="Eliminar proyecto" textoAccion="Eliminar" alConfirmar={eliminar} alCerrar={() => setEliminando(null)}>
          Se borra <strong>{eliminando.nombre}</strong> con sus archivos y no se puede deshacer. La dueña
          recibe un aviso. Si el problema es el contenido, ocultarlo con un motivo es más reversible.
        </Confirmar>
      )}

      <Aviso texto={mensaje} />
    </>
  )
}

/** Pide el motivo antes de ocultar: la dueña lo recibe en su aviso. */
function OcultarProyecto({ proyecto, alCerrar, alConfirmar }) {
  const [motivo, setMotivo] = useState('')
  const valido = motivo.trim().length > 0 && motivo.length <= 500

  return (
    <Ventana titulo={`Ocultar "${proyecto.nombre}"`} alCerrar={alCerrar}>
      <form onSubmit={(e) => { e.preventDefault(); if (valido) alConfirmar(motivo.trim()) }}>
        <label>
          Motivo (la dueña lo verá en su aviso)
          <textarea value={motivo} maxLength={500} rows={4} onChange={(e) => setMotivo(e.target.value)} />
        </label>
        <p className="ayuda">{motivo.length}/500</p>
        <div className="acciones">
          <button type="submit" className="principal" disabled={!valido}>Ocultar proyecto</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}

/** Los comentarios más recientes, con la opción de borrar. */
function ModerarComentarios() {
  const [comentarios, setComentarios] = useState(null)
  const [error, setError] = useState(null)
  const { texto: mensaje, avisar } = useAviso()
  const [eliminando, setEliminando] = useState(null)

  const cargar = useCallback(() => {
    comentariosParaModerar().then(setComentarios).catch((e) => setError(e.message))
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const eliminar = async () => {
    setError(null)
    try {
      await eliminarComentarioModerado(eliminando.id)
      avisar('Comentario eliminado. Quien lo escribió recibe un aviso.')
      cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setEliminando(null)
    }
  }

  if (error && !comentarios) return <div className="error" role="alert">{error}</div>
  if (!comentarios) return <EsqueletoLista texto="Cargando comentarios" />

  return (
    <>
      {error && <div className="error" role="alert">{error}</div>}
      <p className="ayuda">Los 50 comentarios más recientes de toda la plataforma.</p>
      {comentarios.length === 0 ? (
        <div className="vacio">Todavía no hay comentarios.</div>
      ) : (
        <div className="lista">
          {comentarios.map((c) => (
            <div key={c.id} className="tarjeta comentario-admin">
              <p>{c.texto}</p>
              <span className="ayuda">{fechaLarga(c.fecha)}</span>
              <button className="secundario peligro" onClick={() => setEliminando(c)}>Eliminar</button>
            </div>
          ))}
        </div>
      )}

      {eliminando && (
        <Confirmar titulo="Eliminar comentario" textoAccion="Eliminar" alConfirmar={eliminar} alCerrar={() => setEliminando(null)}>
          “{eliminando.texto}”. Quien lo escribió recibe un aviso. No se puede deshacer.
        </Confirmar>
      )}

      <Aviso texto={mensaje} />
    </>
  )
}
