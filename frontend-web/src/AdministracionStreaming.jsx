// Las pestañas de Administración que vienen de Kafka (v3): alertas de actividad, registros por
// hora y el estado del streaming (brokers, tópicos, grupos y lag). Todo se refresca solo: lo que
// llega a los tópicos aparece aquí sin que nadie haga nada.
import { useCallback, useEffect, useState } from 'react'
import {
  alertasActividad, clusterKafka, consumidoresKafka, gruposKafka, marcarAlertaRevisada,
  metricasActividad, pausarConsumidoresKafka, reanudarConsumidoresKafka, topicosKafka,
} from './api'
import Aviso, { useAviso } from './Aviso'
import { duracion, fechaLarga, TIPOS_ACTIVIDAD } from './catalogo'
import { Barras, Cifra } from './Cifras'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'

const ALERTAS_CADA = 20_000
const STREAMING_CADA = 5_000

/** Vuelve a pedir cada tantos milisegundos mientras la pestaña esté abierta. */
function useRefresco(cargar, cada) {
  useEffect(() => {
    cargar()
    const reloj = setInterval(cargar, cada)
    return () => clearInterval(reloj)
  }, [cargar, cada])
}

/**
 * Ráfagas que detectó el consumidor del tópico de actividad: alguien comentó, pidió colaboración
 * o valoró muchas veces en pocos minutos. La plataforma no bloquea a nadie sola: avisa aquí.
 */
export function Alertas() {
  const [alertas, setAlertas] = useState(null)
  const [soloPendientes, setSoloPendientes] = useState(true)
  const [error, setError] = useState(null)
  const { texto: mensaje, avisar } = useAviso()

  const cargar = useCallback(() => alertasActividad(soloPendientes)
    .then((lista) => { setAlertas(lista); setError(null) })
    .catch((e) => setError(e.message)), [soloPendientes])
  useRefresco(cargar, ALERTAS_CADA)

  const revisar = async (alerta) => {
    try {
      await marcarAlertaRevisada(alerta.id)
      avisar('Alerta marcada como revisada.')
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <div className="filtros filtros-admin">
        <label className="casilla">
          <input type="checkbox" checked={soloPendientes} onChange={(e) => setSoloPendientes(e.target.checked)} />
          Solo las pendientes
        </label>
        <p className="ayuda crece">Se actualiza sola cada 20 segundos.</p>
      </div>

      {error && <div className="error" role="alert">{error}</div>}
      {!alertas && !error && <EsqueletoLista texto="Buscando alertas" />}
      {alertas?.length === 0 && (
        <div className="vacio">
          <h3>{soloPendientes ? 'No hay alertas pendientes' : 'Todavía no hay alertas'}</h3>
          <p className="ayuda">Aparecen solas cuando alguien repite una acción más de lo normal en pocos minutos.</p>
        </div>
      )}
      {alertas?.length > 0 && (
        <div className="tabla-scroll">
          <TablaAdmin>
            <thead>
              <tr><th>Persona</th><th>Qué repitió</th><th>Cuánto</th><th>Última vez</th><th>Estado</th><th /></tr>
            </thead>
            <tbody>
              {alertas.map((a) => (
                <tr key={a.id}>
                  <td>
                    <strong>{a.persona?.nombre ?? 'Perfil oculto'}</strong>
                    <span className="ayuda">{[a.persona?.carrera, a.persona?.sede].filter(Boolean).join(' · ')}</span>
                  </td>
                  <td>{TIPOS_ACTIVIDAD[a.tipo] ?? a.tipo}</td>
                  <td>{a.cantidad} en {a.ventanaMinutos} min</td>
                  <td>{fechaLarga(a.hasta)}</td>
                  <td>
                    <span className={a.revisada ? 'insignia revisada' : 'insignia pendiente-revision'}>
                      {a.revisada ? 'Revisada' : 'Pendiente'}
                    </span>
                  </td>
                  <td className="acciones-fila">
                    {!a.revisada && <button className="secundario" onClick={() => revisar(a)}>Marcar revisada</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </TablaAdmin>
        </div>
      )}
      <Aviso texto={mensaje} />
    </>
  )
}

/** Registros por hora de los dos tópicos, contados por el grupo analitica-metricas. */
export function Actividad() {
  const [metricas, setMetricas] = useState(null)
  const [error, setError] = useState(null)

  const cargar = useCallback(() => metricasActividad(24)
    .then((m) => { setMetricas(m); setError(null) })
    .catch((e) => setError(e.message)), [])
  useRefresco(cargar, ALERTAS_CADA)

  if (error) return <div className="error">{error}</div>
  if (!metricas) return <EsqueletoLista filas={8} texto="Contando los registros" />

  const sumar = (filas, clave) => Object.entries(filas.reduce((acc, f) => {
    acc[clave(f)] = (acc[clave(f)] ?? 0) + f.cantidad
    return acc
  }, {})).map(([k, cantidad]) => ({ clave: k, cantidad }))

  const interacciones = metricas.filter((m) => m.topico === 'interacciones')
  const actividad = metricas.filter((m) => m.topico === 'actividad')
  const hora = (m) => new Date(m.hora).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  const total = (filas) => filas.reduce((s, f) => s + f.cantidad, 0)

  return (
    <>
      <div className="cifras">
        <Cifra valor={total(interacciones)} texto="Interacciones en 24 h" detalle="duocconecta.contenido.interacciones" />
        <Cifra valor={total(actividad)} texto="Acciones de personas en 24 h" detalle="duocconecta.usuarios.actividad" />
      </div>
      <div className="grilla">
        <Barras titulo="Interacciones por tipo" conteos={sumar(interacciones, (m) => m.tipo)} etiquetas={TIPOS_ACTIVIDAD} />
        <Barras titulo="Interacciones por hora" conteos={sumar(interacciones, hora)}
                ayuda="Hora en que pasó cada cosa, no en que llegó." />
        <Barras titulo="Actividad por tipo" conteos={sumar(actividad, (m) => m.tipo)} etiquetas={TIPOS_ACTIVIDAD} />
        <Barras titulo="Actividad por hora" conteos={sumar(actividad, hora)} />
      </div>
    </>
  )
}

/**
 * El streaming por dentro: brokers, tópicos, grupos y su lag. Pausar los consumidores deja los
 * registros acumulándose (el lag sube); al reanudar se ponen al día solos.
 */
export function Streaming() {
  const [estado, setEstado] = useState(null)
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const { texto: mensaje, avisar } = useAviso()

  const cargar = useCallback(() => Promise.all([clusterKafka(), topicosKafka(), gruposKafka(), consumidoresKafka()])
    .then(([cluster, topicos, grupos, consumidores]) => {
      setEstado({ cluster, topicos, grupos, consumidores })
      setError(null)
    })
    .catch((e) => setError(e.message)), [])
  useRefresco(cargar, STREAMING_CADA)

  const pausados = estado?.consumidores?.some((c) => c.pausado)

  const alternar = async () => {
    setOcupado(true)
    try {
      if (pausados) {
        await reanudarConsumidoresKafka()
        avisar('Consumidores reanudados: siguen desde su último offset.')
      } else {
        await pausarConsumidoresKafka()
        avisar('Consumidores en pausa: lo que llegue se acumula como lag.')
      }
      cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupado(false)
    }
  }

  if (error && !estado) return <div className="error">{error}</div>
  if (!estado) return <EsqueletoLista filas={8} texto="Preguntándole a Kafka" />

  const { cluster, topicos, grupos } = estado
  return (
    <>
      {error && <div className="error" role="alert">{error}</div>}
      <div className="cifras">
        <Cifra valor={cluster.brokers.length} texto="Brokers vivos" alerta={cluster.brokers.length < 3}
               detalle={`Controlador: broker ${cluster.controlador}`} />
        <Cifra valor={topicos.length} texto="Tópicos" />
        <Cifra valor={grupos.reduce((s, g) => s + g.lagTotal, 0)} texto="Lag total"
               alerta={grupos.some((g) => g.lagTotal > 0)} detalle="Registros que esperan a su consumidor" />
      </div>

      <div className="filtros filtros-admin">
        <p className="ayuda crece">
          {pausados
            ? 'Los consumidores están en pausa: siguen en su grupo, pero no leen.'
            : 'Los consumidores están leyendo. Se actualiza cada 5 segundos.'}
        </p>
        <button type="button" className={pausados ? 'principal' : 'secundario'} disabled={ocupado} onClick={alternar}>
          {pausados ? 'Reanudar consumidores' : 'Pausar consumidores'}
        </button>
      </div>

      <h2 className="subtitulo-admin">Grupos de consumidores</h2>
      <div className="tabla-scroll">
        <TablaAdmin>
          <thead><tr><th>Grupo</th><th>Estado</th><th>Lag</th></tr></thead>
          <tbody>
            {grupos.map((g) => (
              <tr key={g.id}>
                <td><strong>{g.id}</strong><span className="ayuda">{g.asignador}</span></td>
                <td>{g.estado}</td>
                <td><span className={g.lagTotal > 0 ? 'insignia pendiente-revision' : 'insignia revisada'}>{g.lagTotal}</span></td>
              </tr>
            ))}
          </tbody>
        </TablaAdmin>
      </div>

      <h2 className="subtitulo-admin">Tópicos</h2>
      <div className="tabla-scroll">
        <TablaAdmin>
          <thead><tr><th>Tópico</th><th>Réplicas</th><th>Retención</th><th>Limpieza</th><th>Registros</th></tr></thead>
          <tbody>
            {topicos.map((t) => (
              <tr key={t.nombre}>
                <td><strong>{t.nombre}</strong>{t.protegido && <span className="ayuda">Topología base</span>}</td>
                <td>{t.replicacion}</td>
                <td>{duracion(t.configuracion['retention.ms'])}</td>
                <td>{t.configuracion['cleanup.policy']}</td>
                <td>{t.registros}</td>
              </tr>
            ))}
          </tbody>
        </TablaAdmin>
      </div>

      <h2 className="subtitulo-admin">Brokers</h2>
      <div className="chips">
        {cluster.brokers.map((b) => (
          <span key={b.id} className="chip">
            Broker {b.id} · {b.host}:{b.puerto}{b.controlador ? ' · controlador' : ''}
          </span>
        ))}
      </div>
      <Aviso texto={mensaje} />
    </>
  )
}
