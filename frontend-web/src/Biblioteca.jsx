// Biblioteca de prompts (v3): buscar, copiar, valorar y hacer la copia propia de un prompt.
// Se actualiza sola cada 20 segundos (HU-25) y marca lo que está en tendencia según Kafka (HU-26).
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  bifurcarPrompt, editarPrompt, eliminarPrompt, listarPrompts, publicarPrompt, registrarCopiaPrompt, tendencias,
} from './api'
import { claimsDelToken } from './auth'
import Aviso, { useAviso } from './Aviso'
import { CATEGORIAS_PROMPT, ESCUELAS, HERRAMIENTAS_IA, ORDENES_PROMPT } from './catalogo'
import Confirmar from './Confirmar'
import DetallePrompt from './DetallePrompt'
import FormularioPrompt from './FormularioPrompt'
import TarjetaPrompt from './TarjetaPrompt'

const CADA = 20_000
const EN_TENDENCIA = 3
const SIN_FILTROS = { texto: '', categoria: '', carrera: '', herramienta: '', orden: 'RECIENTES' }

export default function Biblioteca({ foco, alUsarFoco }) {
  const [prompts, setPrompts] = useState(null)
  const [puntajes, setPuntajes] = useState(new Map())
  const [miOid, setMiOid] = useState(null)
  const [filtros, setFiltros] = useState(SIN_FILTROS)
  const [texto, setTexto] = useState('')
  const [error, setError] = useState(null)
  const [nuevos, setNuevos] = useState(new Set())
  const conocidos = useRef(null)
  const [abierto, setAbierto] = useState(null)        // id del prompt en el panel
  const [editando, setEditando] = useState(null)      // null · {} nuevo · prompt
  const [esCopia, setEsCopia] = useState(false)
  const [borrando, setBorrando] = useState(null)
  const { texto: aviso, avisar, cerrar: cerrarAviso } = useAviso()

  useEffect(() => {
    claimsDelToken().then((c) => setMiOid(c.oid ?? c.sub)).catch(() => setMiOid(null))
  }, [])

  // El texto se busca medio segundo después de dejar de escribir, no en cada tecla.
  useEffect(() => {
    const espera = setTimeout(() => setFiltros((f) => ({ ...f, texto })), 500)
    return () => clearTimeout(espera)
  }, [texto])

  const cargar = useCallback(() => {
    const { orden, ...resto } = filtros
    return listarPrompts({ ...resto, orden: orden === 'TENDENCIA' ? 'RECIENTES' : orden }).then((lista) => {
      if (conocidos.current) {
        const recien = lista.filter((p) => !conocidos.current.has(p.id)).map((p) => p.id)
        if (recien.length) setNuevos((n) => new Set([...n, ...recien]))
      }
      conocidos.current = new Set(lista.map((p) => p.id))
      setPrompts(lista)
      setError(null)
    }).catch((e) => { setPrompts((p) => p ?? []); setError(e.message) })
  }, [filtros])

  // La tendencia es un extra: si ms-analitica no responde, la biblioteca se ve igual.
  const cargarTendencia = useCallback(() => tendencias('PROMPT', 50)
    .then((lista) => setPuntajes(new Map(lista.map((t) => [t.recursoId, t.puntaje]))))
    .catch(() => setPuntajes(new Map())), [])

  useEffect(() => {
    cargar()
    cargarTendencia()
    const reloj = setInterval(() => { cargar(); cargarTendencia() }, CADA)
    return () => clearInterval(reloj)
  }, [cargar, cargarTendencia])

  // Un enlace compartido (#prompt-<id>) abre el prompt al llegar.
  useEffect(() => {
    if (!foco?.promptId) return
    setAbierto(foco.promptId)
    alUsarFoco()
  }, [foco, alUsarFoco])

  const enTendencia = useMemo(() => new Set((prompts ?? [])
    .filter((p) => puntajes.get(p.id) > 0)
    .sort((a, b) => puntajes.get(b.id) - puntajes.get(a.id))
    .slice(0, EN_TENDENCIA).map((p) => p.id)), [prompts, puntajes])

  const visibles = useMemo(() => {
    if (filtros.orden !== 'TENDENCIA' || !prompts) return prompts ?? []
    return [...prompts].sort((a, b) => (puntajes.get(b.id) ?? 0) - (puntajes.get(a.id) ?? 0))
  }, [prompts, puntajes, filtros.orden])

  /** Copia al portapapeles y le avisa a la analítica. */
  const copiar = async (prompt) => {
    try {
      await navigator.clipboard.writeText(prompt.contenido)
      avisar('Prompt copiado. Pégalo en tu herramienta de IA.')
      registrarCopiaPrompt(prompt.id).catch(() => { /* la copia ya está hecha; la señal es un extra */ })
    } catch {
      setError('El navegador no dejó copiar. Selecciona el texto en el detalle y cópialo a mano.')
    }
  }

  const bifurcar = async (prompt) => {
    try {
      const copia = await bifurcarPrompt(prompt.id)
      setAbierto(null)
      setEsCopia(true)
      setEditando(copia)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const guardar = async (datos) => {
    try {
      if (datos.id) {
        await editarPrompt(datos.id, datos)
        avisar(esCopia ? 'Tu copia quedó guardada.' : 'Prompt actualizado.')
      } else {
        await publicarPrompt(datos)
        avisar('Prompt publicado en la biblioteca.')
      }
      setEditando(null)
      setEsCopia(false)
      cargar()
    } catch (e) {
      setError(e.message)
    }
  }

  const borrar = async () => {
    try {
      await eliminarPrompt(borrando.id)
      setAbierto(null)
      avisar('Prompt eliminado.')
      cargar()
    } catch (e) {
      setError(e.message)
    } finally {
      setBorrando(null)
    }
  }

  const cambiar = (campo) => (e) => setFiltros({ ...filtros, [campo]: e.target.value })
  const hayFiltros = texto || filtros.categoria || filtros.carrera || filtros.herramienta

  return (
    <section>
      <div className="titulo-pagina">
        <div>
          <h1>Biblioteca de prompts</h1>
          <p>Prompts que le sirvieron a alguien de la comunidad, listos para copiar o adaptar.</p>
        </div>
        <div className="acciones-pagina">
          <button type="button" className="principal" onClick={() => { setEsCopia(false); setEditando({}) }}>
            Publicar prompt
          </button>
        </div>
      </div>

      {error && <div className="error" role="alert">{error}</div>}

      <section className="filtros" aria-label="Filtros de la biblioteca">
        <input className="buscador" type="search" aria-label="Buscar prompts"
               placeholder="Buscar por título o para qué sirve" value={texto} onChange={(e) => setTexto(e.target.value)} />
        <select className="ancho-chico" aria-label="Categoría" value={filtros.categoria} onChange={cambiar('categoria')}>
          <option value="">Toda categoría</option>
          {Object.entries(CATEGORIAS_PROMPT).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <select className="ancho-medio" aria-label="Carrera" value={filtros.carrera} onChange={cambiar('carrera')}>
          <option value="">Toda carrera</option>
          {ESCUELAS.map((e) => (
            <optgroup key={e.nombre} label={e.nombre}>
              {e.carreras.map((c) => <option key={c} value={c}>{c}</option>)}
            </optgroup>
          ))}
        </select>
        <select className="ancho-chico" aria-label="Herramienta de IA" value={filtros.herramienta} onChange={cambiar('herramienta')}>
          <option value="">Toda herramienta</option>
          {HERRAMIENTAS_IA.map((h) => <option key={h} value={h}>{h}</option>)}
        </select>
        <select className="ancho-minimo" aria-label="Orden" value={filtros.orden} onChange={cambiar('orden')}>
          {Object.entries(ORDENES_PROMPT).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <p className="resumen" role="status"><strong>{visibles.length}</strong> prompts</p>
        {hayFiltros && (
          <button type="button" className="enlace" onClick={() => { setTexto(''); setFiltros(SIN_FILTROS) }}>Limpiar</button>
        )}
      </section>

      {prompts === null ? (
        <div className="cargando">Abriendo la biblioteca…</div>
      ) : visibles.length === 0 ? (
        <div className="vacio">
          <div className="logo" aria-hidden="true" />
          <h3>{hayFiltros ? 'Ningún prompt coincide' : 'Aún no hay prompts publicados'}</h3>
          <p className="ayuda">Publica uno que te haya servido: a alguien de otra carrera le puede ahorrar una tarde.</p>
          <div className="acciones centradas">
            <button type="button" className="principal" onClick={() => setEditando({})}>Publicar prompt</button>
          </div>
        </div>
      ) : (
        <div className="grilla">
          {visibles.map((p) => (
            <TarjetaPrompt key={p.id} prompt={p} esMio={p.autorId === miOid} esNuevo={nuevos.has(p.id)}
                           enTendencia={enTendencia.has(p.id)} alAbrir={() => setAbierto(p.id)} alCopiar={() => copiar(p)} />
          ))}
        </div>
      )}

      {abierto && (
        <DetallePrompt
          id={abierto}
          miOid={miOid}
          alCopiar={copiar}
          alBifurcar={bifurcar}
          alEditar={(p) => { setAbierto(null); setEsCopia(false); setEditando(p) }}
          alBorrar={setBorrando}
          alAbrirOtro={setAbierto}
          alCerrar={() => setAbierto(null)}
        />
      )}

      {editando && (
        <FormularioPrompt prompt={editando.id ? editando : null} esCopia={esCopia} alGuardar={guardar}
                          alCerrar={() => { setEditando(null); setEsCopia(false) }} />
      )}

      {borrando && (
        <Confirmar titulo="Eliminar prompt" textoAccion="Eliminar" alConfirmar={borrar} alCerrar={() => setBorrando(null)}>
          Se borra <strong>{borrando.titulo}</strong>. Las copias que otras personas hicieron siguen, sin el enlace.
        </Confirmar>
      )}

      <Aviso texto={aviso} alCerrar={cerrarAviso} />
    </section>
  )
}
