// Kafka desde Administración: el estado del clúster, los tópicos con su detalle por partición y
// su configuración, y los grupos con su lag. Todo pasa por ms-mensajeria (AdminClient); lo mismo
// se ve en Kafka UI. Se refresca cada 5 segundos para que el lag se vea moverse.
import { useCallback, useEffect, useState } from 'react'
import {
  aumentarParticionesKafka, clusterKafka, configurarTopicoKafka, consumidoresKafka, crearTopicoKafka,
  eliminarTopicoKafka, grupoKafka, gruposKafka, moverOffsetsKafka, pausarConsumidoresKafka,
  reanudarConsumidoresKafka, topicoKafka, topicosKafka,
} from './api'
import Aviso, { useAviso } from './Aviso'
import { duracion } from './catalogo'
import { Cifra } from './Cifras'
import Confirmar from './Confirmar'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'
import Ventana from './Ventana'

const CADA = 5_000
const RETENCIONES = { 3_600_000: '1 hora', 86_400_000: '24 horas', 259_200_000: '3 días', 604_800_000: '7 días', 1_209_600_000: '14 días' }

export default function AdminKafka() {
  const [estado, setEstado] = useState(null)
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [creando, setCreando] = useState(false)
  const [topico, setTopico] = useState(null)      // nombre abierto en detalle
  const [grupo, setGrupo] = useState(null)        // id abierto en detalle
  const [borrando, setBorrando] = useState(null)
  const { texto: mensaje, avisar } = useAviso()

  const cargar = useCallback(() => Promise.all([clusterKafka(), topicosKafka(), gruposKafka(), consumidoresKafka()])
    .then(([cluster, topicos, grupos, consumidores]) => { setEstado({ cluster, topicos, grupos, consumidores }); setError(null) })
    .catch((e) => setError(e.message)), [])

  useEffect(() => {
    cargar()
    const reloj = setInterval(cargar, CADA)
    return () => clearInterval(reloj)
  }, [cargar])

  const actuar = async (accion, texto) => {
    setOcupado(true)
    try {
      await accion()
      avisar(texto)
      cargar()
      return true
    } catch (e) {
      setError(e.message)
      return false
    } finally {
      setOcupado(false)
    }
  }

  if (error && !estado) return <div className="error">{error}</div>
  if (!estado) return <EsqueletoLista filas={8} texto="Preguntándole a Kafka" />

  const { cluster, topicos, grupos } = estado
  const pausados = estado.consumidores?.some((c) => c.pausado)
  const lagTotal = grupos.reduce((s, g) => s + g.lagTotal, 0)

  return (
    <>
      {error && <div className="error" role="alert">{error}</div>}
      <div className="cifras">
        <Cifra valor={cluster.brokers.length} texto="Brokers vivos" alerta={cluster.brokers.length < 3}
               detalle={`Controlador: broker ${cluster.controlador}`} />
        <Cifra valor={topicos.length} texto="Tópicos" />
        <Cifra valor={grupos.length} texto="Grupos de consumidores" />
        <Cifra valor={lagTotal} texto="Lag total" alerta={lagTotal > 0} detalle="Registros que esperan a su consumidor" />
      </div>

      <div className="filtros filtros-admin">
        <p className="ayuda crece">
          {pausados ? 'Los consumidores de ms-analitica están en pausa: siguen en su grupo, pero no leen.'
            : 'Se actualiza cada 5 segundos. Pausa los consumidores para ver subir el lag.'}
        </p>
        <button type="button" className={pausados ? 'principal' : 'secundario'} disabled={ocupado}
                onClick={() => actuar(pausados ? reanudarConsumidoresKafka : pausarConsumidoresKafka,
                  pausados ? 'Consumidores reanudados: siguen desde su último offset.' : 'Consumidores en pausa.')}>
          {pausados ? 'Reanudar consumidores' : 'Pausar consumidores'}
        </button>
      </div>

      <div className="titulo-seccion-admin">
        <h2 className="subtitulo-admin">Tópicos</h2>
        <button type="button" className="principal" onClick={() => setCreando(true)}>Crear tópico</button>
      </div>
      <div className="tabla-scroll">
        <TablaAdmin>
          <thead><tr><th>Tópico</th><th>Réplicas</th><th>Retención</th><th>Limpieza</th><th>Registros</th><th /></tr></thead>
          <tbody>
            {topicos.map((t) => (
              <tr key={t.nombre}>
                <td><strong>{t.nombre}</strong>{t.protegido && <span className="ayuda">Topología base</span>}</td>
                <td>{t.replicacion}</td>
                <td>{duracion(t.configuracion['retention.ms'])}</td>
                <td>{t.configuracion['cleanup.policy']}</td>
                <td>{t.registros}</td>
                <td className="acciones-fila">
                  <button className="secundario" onClick={() => setTopico(t.nombre)}>Detalle</button>
                  {!t.protegido && <button className="secundario peligro" onClick={() => setBorrando(t.nombre)}>Eliminar</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </TablaAdmin>
      </div>

      <h2 className="subtitulo-admin">Grupos de consumidores</h2>
      <div className="tabla-scroll">
        <TablaAdmin>
          <thead><tr><th>Grupo</th><th>Estado</th><th>Lag</th><th /></tr></thead>
          <tbody>
            {grupos.map((g) => (
              <tr key={g.id}>
                <td><strong>{g.id}</strong><span className="ayuda">{g.asignador}</span></td>
                <td>{g.estado}</td>
                <td><span className={g.lagTotal > 0 ? 'insignia pendiente-revision' : 'insignia revisada'}>{g.lagTotal}</span></td>
                <td className="acciones-fila"><button className="secundario" onClick={() => setGrupo(g.id)}>Detalle</button></td>
              </tr>
            ))}
          </tbody>
        </TablaAdmin>
      </div>

      <h2 className="subtitulo-admin">Brokers</h2>
      <div className="chips">
        {cluster.brokers.map((b) => (
          <span key={b.id} className="chip">Broker {b.id} · {b.host}:{b.puerto}{b.controlador ? ' · controlador' : ''}</span>
        ))}
      </div>

      {creando && (
        <CrearTopico alCerrar={() => setCreando(false)} brokers={cluster.brokers.length}
                     alCrear={async (datos) => { if (await actuar(() => crearTopicoKafka(datos), `Tópico ${datos.nombre} creado.`)) setCreando(false) }} />
      )}
      {topico && <DetalleTopico nombre={topico} alCerrar={() => setTopico(null)} alAvisar={avisar} alCambiar={cargar} />}
      {grupo && <DetalleGrupo id={grupo} alCerrar={() => setGrupo(null)} alAvisar={avisar} />}
      {borrando && (
        <Confirmar titulo="Eliminar tópico" textoAccion="Eliminar" alCerrar={() => setBorrando(null)}
                   alConfirmar={() => { const n = borrando; setBorrando(null); actuar(() => eliminarTopicoKafka(n), `Tópico ${n} eliminado.`) }}>
          Se borra <strong>{borrando}</strong> con todos sus registros. No se puede deshacer.
        </Confirmar>
      )}
      <Aviso texto={mensaje} />
    </>
  )
}

/** Formulario para un tópico nuevo. */
function CrearTopico({ brokers, alCrear, alCerrar }) {
  const [datos, setDatos] = useState({ nombre: 'duocconecta.', particiones: 3, replicacion: Math.min(3, brokers),
    retencion: '86400000', limpieza: 'delete' })
  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value })
  const valido = /^duocconecta\.[a-z0-9]+([.-][a-z0-9]+)*$/.test(datos.nombre)
  return (
    <Ventana titulo="Crear tópico" alCerrar={alCerrar}>
      <form onSubmit={(e) => {
        e.preventDefault()
        if (!valido) return
        alCrear({ nombre: datos.nombre, particiones: Number(datos.particiones), replicacion: Number(datos.replicacion),
          configuracion: { 'retention.ms': datos.retencion, 'cleanup.policy': datos.limpieza } })
      }}>
        <label>
          Nombre
          <input value={datos.nombre} onChange={cambiar('nombre')} className={valido ? '' : 'malo'} />
          <span className="ayuda">Empieza con «duocconecta.» y usa minúsculas, números, puntos o guiones.</span>
        </label>
        <div className="dos-columnas">
          <label>Particiones<input type="number" min={1} max={50} value={datos.particiones} onChange={cambiar('particiones')} /></label>
          <label>Factor de replicación<input type="number" min={1} max={brokers} value={datos.replicacion} onChange={cambiar('replicacion')} />
            <span className="ayuda">Hasta {brokers}: una copia por broker.</span></label>
        </div>
        <div className="dos-columnas">
          <label>Retención
            <select value={datos.retencion} onChange={cambiar('retencion')}>
              {Object.entries(RETENCIONES).map(([ms, t]) => <option key={ms} value={ms}>{t}</option>)}
            </select>
          </label>
          <label>Limpieza
            <select value={datos.limpieza} onChange={cambiar('limpieza')}>
              <option value="delete">delete: se borra al vencer</option>
              <option value="compact">compact: queda el último por clave</option>
              <option value="compact,delete">compact y delete</option>
            </select>
          </label>
        </div>
        <div className="acciones">
          <button type="submit" className="principal" disabled={!valido}>Crear</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}

/** Un tópico por dentro: particiones con su líder e ISR, y la retención y las particiones editables. */
function DetalleTopico({ nombre, alCerrar, alAvisar, alCambiar }) {
  const [t, setT] = useState(null)
  const [error, setError] = useState(null)
  const [retencion, setRetencion] = useState('')
  const [total, setTotal] = useState('')

  const cargar = useCallback(() => topicoKafka(nombre).then((d) => {
    setT(d); setRetencion(d.configuracion['retention.ms'] ?? ''); setTotal(String(d.particiones.length + 1))
  }).catch((e) => setError(e.message)), [nombre])
  useEffect(() => { cargar() }, [cargar])

  const aplicar = async (accion, texto) => {
    setError(null)
    try {
      await accion()
      alAvisar(texto)
      cargar()
      alCambiar()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <Ventana titulo={nombre} alCerrar={alCerrar} ancha enfocarCampo={false}>
      <div className="cuerpo-modal">
        {error && <div className="error" role="alert">{error}</div>}
        {!t ? <EsqueletoLista texto="Leyendo el tópico" /> : (
          <>
            <p className="ayuda">{t.particiones.length} particiones · RF {t.replicacion} · {t.registros} registros guardados
              {t.protegido && ' · topología base: no se borra ni cambia de particiones'}</p>
            <div className="tabla-scroll">
              <TablaAdmin>
                <thead><tr><th>Partición</th><th>Líder</th><th>Réplicas</th><th>ISR</th><th>Offsets</th></tr></thead>
                <tbody>
                  {t.particiones.map((p) => (
                    <tr key={p.numero}>
                      <td>P{p.numero}</td>
                      <td>{p.lider === -1 ? <span className="insignia pendiente-revision">sin líder</span> : `broker ${p.lider}`}</td>
                      <td>{p.replicas.join(', ')}</td>
                      <td><span className={p.sincronizadas.length < p.replicas.length ? 'insignia pendiente-revision' : 'insignia revisada'}>
                        {p.sincronizadas.join(', ')}</span></td>
                      <td className="mono">{p.offsetInicial} → {p.offsetFinal}</td>
                    </tr>
                  ))}
                </tbody>
              </TablaAdmin>
            </div>
            <div className="dos-columnas">
              <form onSubmit={(e) => { e.preventDefault(); aplicar(() => configurarTopicoKafka(nombre, { 'retention.ms': retencion }), `Retención de ${nombre}: ${duracion(retencion)}.`) }}>
                <label>Retención
                  <select value={retencion} onChange={(e) => setRetencion(e.target.value)}>
                    {!RETENCIONES[retencion] && <option value={retencion}>{duracion(retencion)}</option>}
                    {Object.entries(RETENCIONES).map(([ms, x]) => <option key={ms} value={ms}>{x}</option>)}
                  </select>
                </label>
                <button type="submit" className="secundario">Cambiar retención</button>
              </form>
              {!t.protegido && (
                <form onSubmit={(e) => { e.preventDefault(); aplicar(() => aumentarParticionesKafka(nombre, Number(total)), `${nombre} ahora tiene ${total} particiones.`) }}>
                  <label>Particiones (solo aumentar)
                    <input type="number" min={t.particiones.length + 1} max={50} value={total} onChange={(e) => setTotal(e.target.value)} />
                  </label>
                  <button type="submit" className="secundario">Aumentar particiones</button>
                </form>
              )}
            </div>
            {t.protegido && (
              <p className="ayuda">Las particiones de un tópico base no se cambian: la misma clave iría a otra partición y se
                rompería el orden por recurso o por persona.</p>
            )}
          </>
        )}
      </div>
    </Ventana>
  )
}

/** Un grupo por dentro: quién atiende cada partición, dónde va y cuánto le falta. */
function DetalleGrupo({ id, alCerrar, alAvisar }) {
  const [g, setG] = useState(null)
  const [error, setError] = useState(null)

  const cargar = useCallback(() => grupoKafka(id).then(setG).catch((e) => setError(e.message)), [id])
  useEffect(() => {
    cargar()
    const reloj = setInterval(cargar, CADA)
    return () => clearInterval(reloj)
  }, [cargar])

  const mover = async (destino) => {
    setError(null)
    try {
      setG(await moverOffsetsKafka(id, { destino }))
      alAvisar(destino === 'INICIO' ? 'El grupo releerá todo lo que la retención conserva.' : 'El grupo salta lo pendiente.')
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <Ventana titulo={`Grupo ${id}`} alCerrar={alCerrar} ancha enfocarCampo={false}>
      <div className="cuerpo-modal">
        {error && <div className="error" role="alert">{error}</div>}
        {!g ? <EsqueletoLista texto="Leyendo el grupo" /> : (
          <>
            <p className="ayuda">{g.estado} · {g.miembros.length} consumidores · asignador {g.asignador || '—'} · lag {g.lagTotal}</p>
            <div className="tabla-scroll">
              <TablaAdmin>
                <thead><tr><th>Partición</th><th>Confirmado</th><th>Final</th><th>Lag</th><th>Consumidor</th></tr></thead>
                <tbody>
                  {g.particiones.map((p) => (
                    <tr key={`${p.topico}-${p.particion}`}>
                      <td>{p.topico.replace('duocconecta.', '')} · P{p.particion}</td>
                      <td className="mono">{p.confirmado}</td>
                      <td className="mono">{p.offsetFinal}</td>
                      <td><span className={p.lag > 0 ? 'insignia pendiente-revision' : 'insignia revisada'}>{p.lag}</span></td>
                      <td>{p.consumidor || <span className="ayuda">sin consumidor</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </TablaAdmin>
            </div>
            <div className="tarjeta bloque">
              <p className="rotulo">Mover offsets</p>
              <p className="ayuda sin-margen">Solo con el grupo vacío: hay que detener ms-analitica (pausar no basta, el grupo sigue activo).</p>
              <div className="acciones">
                <button type="button" className="secundario" disabled={g.miembros.length > 0} onClick={() => mover('INICIO')}>Releer desde el inicio</button>
                <button type="button" className="secundario" disabled={g.miembros.length > 0} onClick={() => mover('FIN')}>Saltar al final</button>
              </div>
            </div>
          </>
        )}
      </div>
    </Ventana>
  )
}
