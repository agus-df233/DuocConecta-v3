// Panel lateral con un prompt completo: el texto para copiar, las estrellas y la copia propia.
// Abrirlo es una vista y copiarlo, una copia: las dos van a Kafka y alimentan la tendencia.
import { useEffect, useId, useState } from 'react'
import { interaccionesDe, obtenerPrompt, registrarVistaPrompt, valorarPrompt } from './api'
import { CATEGORIAS_PROMPT, fechaCorta } from './catalogo'
import { ElegirEstrellas, PromedioEstrellas } from './Estrellas'
import { useAtraparFoco } from './Ventana'

export default function DetallePrompt({ id, miOid, alCopiar, alBifurcar, alEditar, alBorrar, alAbrirOtro, alCerrar }) {
  const [prompt, setPrompt] = useState(null)
  const [interes, setInteres] = useState(null)
  const [error, setError] = useState(null)
  const [valorando, setValorando] = useState(false)
  const caja = useAtraparFoco(alCerrar)
  const titulo = useId()

  useEffect(() => {
    let vigente = true
    setPrompt(null)
    setError(null)
    obtenerPrompt(id).then((p) => { if (vigente) setPrompt(p) }).catch((e) => { if (vigente) setError(e.message) })
    // La vista va a Kafka; el conteo se vuelve a pedir cuando el consumidor ya la procesó.
    const leer = () => interaccionesDe(id).then((r) => { if (vigente) setInteres(r) }).catch(() => {})
    registrarVistaPrompt(id).catch(() => { /* una vista perdida no es un error para nadie */ })
    leer()
    const despues = setTimeout(leer, 2500)
    return () => { vigente = false; clearTimeout(despues) }
  }, [id])

  const valorar = async (estrellas) => {
    setValorando(true)
    setError(null)
    try {
      setPrompt({ ...(await valorarPrompt(id, estrellas)), autor: prompt.autor, origen: prompt.origen })
    } catch (e) {
      setError(e.message)
    } finally {
      setValorando(false)
    }
  }

  const conteos = interes?.conteos ?? {}
  // Se decide con el prompt cargado: el que se abre puede no estar en la lista filtrada.
  const esMio = prompt?.autorId === miOid

  return (
    <>
      <div className="telon" onClick={alCerrar} />
      <aside className="drawer" role="dialog" aria-modal="true" aria-labelledby={titulo} ref={caja} tabIndex={-1}>
        <div className="drawer-cabecera">
          <div className="crece">
            {prompt && <span className="insignia categoria">{CATEGORIAS_PROMPT[prompt.categoria] ?? prompt.categoria}</span>}
            <h2 id={titulo}>{prompt?.titulo ?? 'Cargando…'}</h2>
            {prompt && (
              <p>
                {[prompt.autor?.nombre ?? 'Alguien de la comunidad', prompt.carrera, prompt.herramienta,
                  fechaCorta(prompt.creadoEn)].filter(Boolean).join(' · ')}
              </p>
            )}
            {interes && (
              <p className="interes" aria-live="polite">
                {`${conteos.VISTA ?? 0} vistas · ${conteos.COPIA ?? 0} copias esta semana`}
              </p>
            )}
          </div>
          <button type="button" className="cerrar" onClick={alCerrar} aria-label="Cerrar">×</button>
        </div>

        <div className="drawer-cuerpo">
          {error && <div className="error" role="alert">{error}</div>}
          {prompt && (
            <>
              <p>{prompt.proposito}</p>

              {prompt.origen && (
                <p className="ayuda">
                  Bifurcado de{' '}
                  <button type="button" className="enlace" onClick={() => alAbrirOtro(prompt.origen.id)}>
                    «{prompt.origen.titulo}»
                  </button>
                  {prompt.origen.autor?.nombre && ` de ${prompt.origen.autor.nombre}`}
                </p>
              )}
              {prompt.oculto && prompt.motivoOcultamiento && (
                <p className="motivo-moderacion">Oculto por moderación. Motivo: {prompt.motivoOcultamiento}</p>
              )}

              <div className="tarjeta bloque">
                <p className="rotulo">El prompt</p>
                <pre className="prompt-texto">{prompt.contenido}</pre>
                <div className="acciones">
                  <button type="button" className="principal" onClick={() => alCopiar(prompt)}>Copiar prompt</button>
                </div>
              </div>

              <div className="fila-valoracion">
                <PromedioEstrellas promedio={prompt.promedio} valoraciones={prompt.valoraciones} />
                <span className="ayuda">
                  {prompt.bifurcaciones === 1 ? '1 copia propia hecha' : `${prompt.bifurcaciones} copias propias hechas`}
                </span>
              </div>

              {esMio ? (
                <div className="acciones">
                  <button type="button" className="secundario" onClick={() => alEditar(prompt)}>Editar</button>
                  <button type="button" className="secundario peligro" onClick={() => alBorrar(prompt)}>Eliminar</button>
                </div>
              ) : (
                <>
                  <ElegirEstrellas valor={prompt.miValoracion ?? 0} alElegir={valorar} deshabilitado={valorando} />
                  <div className="acciones">
                    <button type="button" className="secundario" onClick={() => alBifurcar(prompt)}>
                      Hacer mi copia
                    </button>
                  </div>
                  <p className="ayuda">Tu copia nace como borrador y queda enlazada a este prompt.</p>
                </>
              )}
            </>
          )}
        </div>
      </aside>
    </>
  )
}
