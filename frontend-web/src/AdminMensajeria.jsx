// Mensajería de la EP2 desde Administración: las colas de SQS y RabbitMQ, sus DLQ y los
// consumidores de ms-notificaciones. Cada sección carga por su cuenta: si AWS o RabbitMQ no
// responden, esa sección lo dice y las demás siguen funcionando.
import { useCallback, useEffect, useState } from 'react'
import {
  colasRabbit, colasSqs, consumidoresMensajeria, crearBindingRabbit, crearColaRabbit, descartes,
  eliminarBindingRabbit, eliminarColaRabbit, mensajesDescartados, pausarConsumidoresMensajeria,
  purgarColaRabbit, purgarColaSqs, reanudarConsumidoresMensajeria, redriveRabbit, redriveSqs,
} from './api'
import Aviso, { useAviso } from './Aviso'
import Confirmar from './Confirmar'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'
import Ventana from './Ventana'

/** Carga una sección por separado, con su propio error. */
function useSeccion(pedir) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const cargar = useCallback(() => pedir().then((d) => { setDatos(d); setError(null) }).catch((e) => setError(e.message)), [pedir])
  useEffect(() => { cargar() }, [cargar])
  return { datos, error, cargar }
}

export default function AdminMensajeria() {
  const sqs = useSeccion(colasSqs)
  const rabbit = useSeccion(colasRabbit)
  const dlq = useSeccion(descartes)
  const consumidores = useSeccion(consumidoresMensajeria)
  const { texto: mensaje, avisar } = useAviso()
  const [confirmar, setConfirmar] = useState(null)   // { titulo, texto, accion, exito }
  const [viendo, setViendo] = useState(null)         // cola de descarte abierta
  const [creandoCola, setCreandoCola] = useState(false)
  const [binding, setBinding] = useState(null)       // 'crear' | 'eliminar'
  const [error, setError] = useState(null)

  const actuar = async (accion, exito, recargar) => {
    setError(null)
    try {
      const r = await accion()
      avisar(typeof exito === 'function' ? exito(r) : exito)
      recargar?.forEach((s) => s.cargar())
      return true
    } catch (e) {
      setError(e.message)
      return false
    }
  }

  const activos = consumidores.datos?.some((c) => c.activo)

  return (
    <>
      {error && <div className="error" role="alert">{error}</div>}

      <h2 className="subtitulo-admin">Consumidores de ms-notificaciones</h2>
      <Seccion seccion={consumidores}>
        {(lista) => (
          <div className="filtros filtros-admin">
            <div className="chips crece">
              {lista.map((c) => (
                <span key={`${c.broker}-${c.id}`} className={c.activo ? 'chip' : 'chip apagado'}>
                  {c.broker} · {c.id} · {c.activo ? 'activo' : 'detenido'}
                </span>
              ))}
            </div>
            <button type="button" className={activos ? 'secundario' : 'principal'}
                    onClick={() => actuar(activos ? pausarConsumidoresMensajeria : reanudarConsumidoresMensajeria,
                      activos ? 'Consumidores detenidos en los dos brokers: los mensajes esperan en sus colas.' : 'Consumidores activos otra vez.',
                      [consumidores, sqs, rabbit])}>
              {activos ? 'Pausar (SQS y RabbitMQ)' : 'Reanudar'}
            </button>
          </div>
        )}
      </Seccion>

      <h2 className="subtitulo-admin">Colas de descarte (DLQ)</h2>
      <Seccion seccion={dlq}>
        {(lista) => (
          <div className="tabla-scroll">
            <TablaAdmin>
              <thead><tr><th>Broker</th><th>Cola</th><th>Mensajes</th><th /></tr></thead>
              <tbody>
                {lista.map((c) => (
                  <tr key={`${c.broker}-${c.nombre}`}>
                    <td>{c.broker}</td>
                    <td><strong>{c.nombre}</strong></td>
                    <td><span className={c.mensajes > 0 ? 'insignia pendiente-revision' : 'insignia revisada'}>{c.mensajes}</span></td>
                    <td className="acciones-fila">
                      {c.broker === 'SQS' && <button className="secundario" onClick={() => setViendo(c.nombre)}>Ver mensajes</button>}
                      {c.mensajes > 0 && (
                        <button className="secundario" onClick={() => actuar(() => (c.broker === 'SQS' ? redriveSqs(c.nombre) : redriveRabbit(c.nombre)),
                          (r) => (r?.movidos != null ? `${r.movidos} mensajes devueltos a su cola.` : 'Redrive iniciado.'), [dlq, sqs, rabbit])}>
                          Redrive
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </TablaAdmin>
          </div>
        )}
      </Seccion>

      <h2 className="subtitulo-admin">Amazon SQS</h2>
      <Seccion seccion={sqs}>
        {(lista) => (
          <div className="tabla-scroll">
            <TablaAdmin>
              <thead><tr><th>Cola</th><th>Visibles</th><th>En vuelo</th><th>DLQ</th><th /></tr></thead>
              <tbody>
                {lista.map((c) => (
                  <tr key={c.nombre}>
                    <td><strong>{c.nombre}</strong>{c.protegida && <span className="ayuda">Topología base</span>}</td>
                    <td>{c.mensajesVisibles}</td>
                    <td>{c.mensajesEnVuelo}</td>
                    <td>{c.colaDescarte ? `${c.colaDescarte} (tras ${c.maxRecepciones})` : '—'}</td>
                    <td className="acciones-fila">
                      <button className="secundario peligro" onClick={() => setConfirmar({
                        titulo: 'Purgar cola', texto: `Se borran todos los mensajes de ${c.nombre}. No se puede deshacer.`,
                        accion: () => actuar(() => purgarColaSqs(c.nombre), `Cola ${c.nombre} purgada.`, [sqs, dlq]),
                      })}>Purgar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </TablaAdmin>
          </div>
        )}
      </Seccion>

      <div className="titulo-seccion-admin">
        <h2 className="subtitulo-admin">RabbitMQ</h2>
        <div className="acciones">
          <button type="button" className="secundario" onClick={() => setBinding('crear')}>Crear binding</button>
          <button type="button" className="secundario" onClick={() => setBinding('eliminar')}>Eliminar binding</button>
          <button type="button" className="principal" onClick={() => setCreandoCola(true)}>Crear cola</button>
        </div>
      </div>
      <Seccion seccion={rabbit}>
        {(lista) => (
          <div className="tabla-scroll">
            <TablaAdmin>
              <thead><tr><th>Cola</th><th>Mensajes</th><th>Consumidores</th><th>DLQ</th><th /></tr></thead>
              <tbody>
                {lista.map((c) => (
                  <tr key={c.nombre}>
                    <td><strong>{c.nombre}</strong>{c.protegida && <span className="ayuda">Topología base</span>}</td>
                    <td>{c.mensajes}</td>
                    <td>{c.consumidores}</td>
                    <td>{c.dlq ?? '—'}</td>
                    <td className="acciones-fila">
                      <button className="secundario peligro" onClick={() => setConfirmar({
                        titulo: 'Purgar cola', texto: `Se borran todos los mensajes de ${c.nombre}.`,
                        accion: () => actuar(() => purgarColaRabbit(c.nombre), (r) => `${r?.borrados ?? 0} mensajes borrados de ${c.nombre}.`, [rabbit, dlq]),
                      })}>Purgar</button>
                      {!c.protegida && (
                        <button className="secundario peligro" onClick={() => setConfirmar({
                          titulo: 'Eliminar cola', texto: `Se elimina ${c.nombre} con sus mensajes.`,
                          accion: () => actuar(() => eliminarColaRabbit(c.nombre), `Cola ${c.nombre} eliminada.`, [rabbit]),
                        })}>Eliminar</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </TablaAdmin>
          </div>
        )}
      </Seccion>
      <p className="ayuda">AMQP no permite listar todas las colas del broker: aquí se ven las de la topología base. Las que crees
        aparecen en la consola de RabbitMQ (puerto 15672).</p>

      {viendo && <MensajesDescartados cola={viendo} alCerrar={() => setViendo(null)} />}
      {creandoCola && (
        <CrearColaRabbit alCerrar={() => setCreandoCola(false)}
                         alCrear={async (datos) => { if (await actuar(() => crearColaRabbit(datos), `Cola ${datos.nombre} creada.`, [rabbit])) setCreandoCola(false) }} />
      )}
      {binding && (
        <FormularioBinding modo={binding} alCerrar={() => setBinding(null)}
                           alEnviar={async (datos) => {
                             const ok = await actuar(() => (binding === 'crear' ? crearBindingRabbit(datos) : eliminarBindingRabbit(datos)),
                               binding === 'crear' ? 'Binding creado.' : 'Binding eliminado.', [rabbit])
                             if (ok) setBinding(null)
                           }} />
      )}
      {confirmar && (
        <Confirmar titulo={confirmar.titulo} textoAccion={confirmar.titulo.split(' ')[0]} alCerrar={() => setConfirmar(null)}
                   alConfirmar={() => { const a = confirmar.accion; setConfirmar(null); a() }}>
          {confirmar.texto}
        </Confirmar>
      )}
      <Aviso texto={mensaje} />
    </>
  )
}

/** Una sección con su carga, su error y su contenido. */
function Seccion({ seccion, children }) {
  if (seccion.error) return <div className="aviso">No disponible: {seccion.error}</div>
  if (!seccion.datos) return <EsqueletoLista filas={3} />
  if (Array.isArray(seccion.datos) && seccion.datos.length === 0) return <p className="ayuda">Nada por aquí.</p>
  return children(seccion.datos)
}

/** Los mensajes de una DLQ de SQS, sin sacarlos de la cola. */
function MensajesDescartados({ cola, alCerrar }) {
  const [mensajes, setMensajes] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => { mensajesDescartados(cola).then(setMensajes).catch((e) => setError(e.message)) }, [cola])
  return (
    <Ventana titulo={cola} alCerrar={alCerrar} ancha enfocarCampo={false}>
      <div className="cuerpo-modal">
        <p className="ayuda">Se leen sin sacarlos de la cola (hasta 10). Para devolverlos, usa Redrive.</p>
        {error && <div className="error" role="alert">{error}</div>}
        {!mensajes && !error && <EsqueletoLista filas={4} />}
        {mensajes?.length === 0 && <p className="ayuda">La cola está vacía.</p>}
        {mensajes?.map((m) => (
          <div key={m.idMensaje} className="tarjeta bloque">
            <p className="rotulo">Motivo: {m.motivo ?? 'agotó sus reintentos'} · recepciones {m.recepciones} · petición {m.idPeticion ?? '—'}</p>
            <pre className="prompt-texto">{m.cuerpo}</pre>
          </div>
        ))}
      </div>
    </Ventana>
  )
}

/** Una cola quorum nueva, con su DLQ enlazada al DLX. */
function CrearColaRabbit({ alCrear, alCerrar }) {
  const [datos, setDatos] = useState({ nombre: 'duocconecta.', conDescarte: true, maxEntregas: 3 })
  const valido = /^duocconecta\.[a-z0-9]+([.-][a-z0-9]+)*$/.test(datos.nombre)
  return (
    <Ventana titulo="Crear cola de RabbitMQ" alCerrar={alCerrar}>
      <form onSubmit={(e) => { e.preventDefault(); if (valido) alCrear({ ...datos, maxEntregas: Number(datos.maxEntregas) }) }}>
        <label>Nombre<input value={datos.nombre} onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} className={valido ? '' : 'malo'} /></label>
        <label className="casilla"><input type="checkbox" checked={datos.conDescarte}
                                          onChange={(e) => setDatos({ ...datos, conDescarte: e.target.checked })} /> Con su DLQ (&lt;nombre&gt;.descartes)</label>
        <label>Entregas antes de ir a la DLQ<input type="number" min={1} max={10} value={datos.maxEntregas}
                                                   onChange={(e) => setDatos({ ...datos, maxEntregas: e.target.value })} /></label>
        <div className="acciones">
          <button type="submit" className="principal" disabled={!valido}>Crear</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}

/** Un binding: qué routing key lleva del exchange a la cola. */
function FormularioBinding({ modo, alEnviar, alCerrar }) {
  const [datos, setDatos] = useState({ exchange: 'duocconecta.eventos', cola: 'duocconecta.', routingKey: '' })
  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value })
  return (
    <Ventana titulo={modo === 'crear' ? 'Crear binding' : 'Eliminar binding'} alCerrar={alCerrar}>
      <form onSubmit={(e) => { e.preventDefault(); alEnviar(datos) }}>
        <label>Exchange<input value={datos.exchange} onChange={cambiar('exchange')} /></label>
        <label>Cola<input value={datos.cola} onChange={cambiar('cola')} /></label>
        <label>Routing key<input value={datos.routingKey} onChange={cambiar('routingKey')} placeholder="proyecto.# o colaboracion.*" /></label>
        <p className="ayuda">En un exchange topic, <code>*</code> reemplaza una palabra y <code>#</code> cero o más.</p>
        <div className="acciones">
          <button type="submit" className={modo === 'crear' ? 'principal' : 'secundario peligro'}>{modo === 'crear' ? 'Crear' : 'Eliminar'}</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}
