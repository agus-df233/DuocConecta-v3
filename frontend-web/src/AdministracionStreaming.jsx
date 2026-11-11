// Las secciones de Administración que vienen de Kafka (v3): alertas de actividad y registros por
// hora. El estado del clúster está en AdminKafka. Todo se refresca solo: lo que llega a los
// tópicos aparece aquí sin que nadie haga nada.
import { useCallback, useEffect, useState } from 'react'
import { alertasActividad, marcarAlertaRevisada, metricasActividad } from './api'
import { FichaPersona } from './AdminPersonas'
import Aviso, { useAviso } from './Aviso'
import { fechaLarga, TIPOS_ACTIVIDAD } from './catalogo'
import { Barras, Cifra } from './Cifras'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'

const ALERTAS_CADA = 20_000

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
  const [ficha, setFicha] = useState(null)
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
                    <button className="secundario" onClick={() => setFicha(a.usuarioId)}>Ver ficha</button>
                    {!a.revisada && <button className="secundario" onClick={() => revisar(a)}>Marcar revisada</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </TablaAdmin>
        </div>
      )}
      {ficha && <FichaPersona oid={ficha} alCerrar={() => setFicha(null)} />}
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
