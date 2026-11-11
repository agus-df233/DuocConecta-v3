// Gestión de personas (solo ADMINISTRADOR): buscar en toda la comunidad, ver la ficha de alguien
// (perfil, proyectos, prompts y alertas, armada por el BFF) y ocultar un perfil por moderación.
// Nunca aparecen teléfono ni redes: esos solo se comparten por consentimiento mutuo.
import { useCallback, useEffect, useId, useState } from 'react'
import { buscarPersonas, fichaPersona, mostrarPersona, ocultarPersona } from './api'
import Aviso, { useAviso } from './Aviso'
import { CATEGORIAS_PROMPT, ESCUELAS, ESTADOS, TIPOS_ACTIVIDAD, fechaCorta, fechaLarga, iniciales } from './catalogo'
import { EsqueletoLista } from './Esqueleto'
import TablaAdmin from './TablaAdmin'
import Ventana, { useAtraparFoco, useCierreAnimado } from './Ventana'

const ESTADOS_PERSONA = { '': 'Todas', VISIBLE: 'Visibles', OCULTA: 'Ocultas por ellas', MODERADA: 'Ocultas por moderación' }
const ROLES = { '': 'Todo rol', ESTUDIANTE: 'Estudiante', PROFESOR: 'Profesor', ACADEMICO: 'Académico' }

export default function AdminPersonas() {
  const [filtros, setFiltros] = useState({ texto: '', carrera: '', rol: '', estado: '', pagina: 0 })
  const [texto, setTexto] = useState('')
  const [pagina, setPagina] = useState(null)
  const [error, setError] = useState(null)
  const [ficha, setFicha] = useState(null)        // oid abierto
  const [ocultando, setOcultando] = useState(null)
  const { texto: mensaje, avisar } = useAviso()

  useEffect(() => {
    const espera = setTimeout(() => setFiltros((f) => ({ ...f, texto, pagina: 0 })), 400)
    return () => clearTimeout(espera)
  }, [texto])

  const cargar = useCallback(() => {
    buscarPersonas({ ...filtros, tamano: 20 }).then((p) => { setPagina(p); setError(null) }).catch((e) => setError(e.message))
  }, [filtros])
  useEffect(() => { cargar() }, [cargar])

  const cambiar = (campo) => (e) => setFiltros({ ...filtros, [campo]: e.target.value, pagina: 0 })

  const actuar = async (accion, texto) => {
    try {
      await accion()
      avisar(texto)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const personas = pagina?.content ?? []
  const total = pagina?.page?.totalElements ?? pagina?.totalElements ?? personas.length
  const paginas = pagina?.page?.totalPages ?? pagina?.totalPages ?? 1

  return (
    <>
      <section className="filtros" aria-label="Buscar personas">
        <input className="buscador" type="search" aria-label="Buscar por nombre o correo"
               placeholder="Buscar por nombre o correo" value={texto} onChange={(e) => setTexto(e.target.value)} />
        <select className="ancho-medio" aria-label="Carrera" value={filtros.carrera} onChange={cambiar('carrera')}>
          <option value="">Toda carrera</option>
          {ESCUELAS.map((e) => (
            <optgroup key={e.nombre} label={e.nombre}>{e.carreras.map((c) => <option key={c} value={c}>{c}</option>)}</optgroup>
          ))}
        </select>
        <select className="ancho-chico" aria-label="Rol" value={filtros.rol} onChange={cambiar('rol')}>
          {Object.entries(ROLES).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <select className="ancho-chico" aria-label="Estado" value={filtros.estado} onChange={cambiar('estado')}>
          {Object.entries(ESTADOS_PERSONA).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <p className="resumen" role="status"><strong>{total}</strong> personas</p>
      </section>

      {error && <div className="error" role="alert">{error}</div>}
      {!pagina ? (!error && <EsqueletoLista texto="Buscando personas" />) : personas.length === 0 ? (
        <div className="vacio"><h3>Nadie coincide</h3><p className="ayuda">Prueba con menos filtros.</p></div>
      ) : (
        <div className="tabla-scroll">
          <TablaAdmin>
            <thead><tr><th>Persona</th><th>Rol</th><th>Carrera</th><th>Estado</th><th>Desde</th><th /></tr></thead>
            <tbody>
              {personas.map((p) => (
                <tr key={p.id}>
                  <td><strong>{p.nombre}</strong><span className="ayuda">{p.correo}</span></td>
                  <td>{ROLES[p.rol] ?? p.rol}</td>
                  <td>{p.carrera ?? '—'}</td>
                  <td><EstadoPersona persona={p} /></td>
                  <td>{fechaCorta(p.creadoEn)}</td>
                  <td className="acciones-fila">
                    <button className="secundario" onClick={() => setFicha(p.oid)}>Ver ficha</button>
                    {p.ocultoPorModeracion
                      ? <button className="secundario" onClick={() => actuar(() => mostrarPersona(p.id), 'Perfil de vuelta en los listados.')}>Mostrar</button>
                      : <button className="secundario peligro" onClick={() => setOcultando(p)}>Ocultar</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </TablaAdmin>
        </div>
      )}

      {paginas > 1 && (
        <div className="paginador">
          <button className="secundario" disabled={filtros.pagina === 0} onClick={() => setFiltros({ ...filtros, pagina: filtros.pagina - 1 })}>Anterior</button>
          <span className="ayuda">Página {filtros.pagina + 1} de {paginas}</span>
          <button className="secundario" disabled={filtros.pagina + 1 >= paginas} onClick={() => setFiltros({ ...filtros, pagina: filtros.pagina + 1 })}>Siguiente</button>
        </div>
      )}

      {ficha && <FichaPersona oid={ficha} alCerrar={() => setFicha(null)} />}
      {ocultando && (
        <MotivoModeracion titulo={`Ocultar a ${ocultando.nombre}`} accion="Ocultar perfil" alCerrar={() => setOcultando(null)}
                          ayuda="Deja de aparecer en los listados y como autora. Puede seguir entrando y ve lo suyo."
                          alConfirmar={(motivo) => { setOcultando(null); actuar(() => ocultarPersona(ocultando.id, motivo), 'Perfil oculto por moderación.') }} />
      )}
      <Aviso texto={mensaje} />
    </>
  )
}

function EstadoPersona({ persona }) {
  if (persona.ocultoPorModeracion) return <span className="insignia moderado" title={persona.motivoModeracion}>Moderada</span>
  if (!persona.visible) return <span className="insignia oculto">Oculta por ella</span>
  return <span className="insignia revisada">Visible</span>
}

/** Pide el motivo antes de una acción de moderación. */
export function MotivoModeracion({ titulo, accion, ayuda, alCerrar, alConfirmar }) {
  const [motivo, setMotivo] = useState('')
  const valido = motivo.trim().length > 0 && motivo.length <= 500
  return (
    <Ventana titulo={titulo} alCerrar={alCerrar}>
      <form onSubmit={(e) => { e.preventDefault(); if (valido) alConfirmar(motivo.trim()) }}>
        {ayuda && <p className="ayuda">{ayuda}</p>}
        <label>
          Motivo
          <textarea value={motivo} maxLength={500} rows={4} onChange={(e) => setMotivo(e.target.value)} />
        </label>
        <p className="ayuda">{motivo.length}/500</p>
        <div className="acciones">
          <button type="submit" className="principal" disabled={!valido}>{accion}</button>
          <button type="button" className="secundario" onClick={alCerrar}>Cancelar</button>
        </div>
      </form>
    </Ventana>
  )
}

/** La ficha de una persona en un panel: lo que juntó el BFF de cuatro servicios. */
export function FichaPersona({ oid, alCerrar }) {
  const [ficha, setFicha] = useState(null)
  const [error, setError] = useState(null)
  const [cerrando, cerrar] = useCierreAnimado(alCerrar)
  const caja = useAtraparFoco(cerrar)
  const titulo = useId()

  useEffect(() => {
    fichaPersona(oid).then(setFicha).catch((e) => setError(e.message))
  }, [oid])

  const p = ficha?.persona
  return (
    <>
      <div className={cerrando ? 'telon cerrando' : 'telon'} onClick={cerrar} />
      <aside className={cerrando ? 'drawer cerrando' : 'drawer'} role="dialog" aria-modal="true" aria-labelledby={titulo} ref={caja} tabIndex={-1}>
        <div className="drawer-cabecera">
          <div className="iniciales grande" aria-hidden="true">{iniciales(p?.nombre)}</div>
          <div className="crece">
            <h2 id={titulo}>{p?.nombre ?? 'Cargando…'}</h2>
            {p && <p>{[p.correo, p.carrera, p.sede].filter(Boolean).join(' · ')}</p>}
          </div>
          <button type="button" className="cerrar" onClick={cerrar} aria-label="Cerrar">×</button>
        </div>
        <div className="drawer-cuerpo">
          {error && <div className="error" role="alert">{error}</div>}
          {!ficha && !error && <EsqueletoLista filas={8} texto="Armando la ficha" />}
          {ficha && (
            <>
              <div className="chips">
                <EstadoPersona persona={p} />
                <span className="chip">{ROLES[p.rol] ?? p.rol}</span>
                <span className="chip">Desde {fechaCorta(p.creadoEn)}</span>
              </div>
              {p.ocultoPorModeracion && <p className="motivo-moderacion">Oculta por moderación: {p.motivoModeracion}</p>}
              {ficha.noDisponible?.length > 0 && (
                <div className="aviso">No se pudo traer: {ficha.noDisponible.join(', ')}. El resto está al día.</div>
              )}
              <BloqueFicha titulo={`Proyectos (${ficha.proyectos.length})`} vacio="No ha publicado proyectos.">
                {ficha.proyectos.map((x) => (
                  <li key={x.id}><strong>{x.nombre}</strong>
                    <span className="ayuda">{ESTADOS[x.estado] ?? x.estado} · {x.visibilidad}{x.oculto ? ' · oculto' : ''}</span></li>
                ))}
              </BloqueFicha>
              <BloqueFicha titulo={`Prompts (${ficha.prompts.length})`} vacio="No ha publicado prompts.">
                {ficha.prompts.map((x) => (
                  <li key={x.id}><strong>{x.titulo}</strong>
                    <span className="ayuda">{CATEGORIAS_PROMPT[x.categoria] ?? x.categoria} · {x.visibilidad === 'PUBLICO' ? 'público' : 'borrador'}{x.oculto ? ' · oculto' : ''}</span></li>
                ))}
              </BloqueFicha>
              <BloqueFicha titulo={`Alertas de actividad (${ficha.alertas.length})`} vacio="Sin alertas.">
                {ficha.alertas.map((a) => (
                  <li key={a.id}><strong>{TIPOS_ACTIVIDAD[a.tipo] ?? a.tipo}: {a.cantidad} en {a.ventanaMinutos} min</strong>
                    <span className="ayuda">{fechaLarga(a.hasta)} · {a.revisada ? 'revisada' : 'pendiente'}</span></li>
                ))}
              </BloqueFicha>
            </>
          )}
        </div>
      </aside>
    </>
  )
}

function BloqueFicha({ titulo, vacio, children }) {
  const lista = Array.isArray(children) ? children : [children]
  return (
    <div className="tarjeta bloque">
      <p className="rotulo">{titulo}</p>
      {lista.length === 0 ? <p className="ayuda sin-margen">{vacio}</p> : <ul className="lista-ficha">{children}</ul>}
    </div>
  )
}
