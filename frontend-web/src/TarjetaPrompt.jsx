// Una tarjeta de la biblioteca: lo justo para saber si el prompt sirve. El texto completo y las
// acciones van en el detalle; copiar está aquí también, porque es lo que más se hace.
import { CATEGORIAS_PROMPT, fechaRelativa, iniciales } from './catalogo'
import { PromedioEstrellas } from './Estrellas'

export default function TarjetaPrompt({ prompt, esMio, esNuevo, enTendencia, alAbrir, alCopiar }) {
  const autor = prompt.autor ?? {}

  const soloEsto = (accion) => (evento) => {
    evento.stopPropagation()
    accion()
  }

  return (
    <article
      className={esNuevo ? 'tarjeta prompt nuevo' : 'tarjeta prompt'}
      tabIndex={0}
      aria-label={prompt.titulo}
      onClick={alAbrir}
      onKeyDown={(e) => {
        if (e.target !== e.currentTarget) return
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); alAbrir() }
      }}
    >
      <div className="etiquetas">
        <span className="insignia categoria">{CATEGORIAS_PROMPT[prompt.categoria] ?? prompt.categoria}</span>
        {esNuevo && <span className="insignia-nuevo">Nuevo</span>}
        {enTendencia && <span className="insignia tendencia">En tendencia</span>}
        {esMio && <span className="insignia propio">Tuyo</span>}
        {esMio && prompt.visibilidad === 'PRIVADO' && <span className="insignia oculto">Borrador</span>}
        <span className="fecha">{fechaRelativa(prompt.creadoEn)}</span>
      </div>

      <h3>{prompt.titulo}</h3>
      <p>{prompt.proposito}</p>
      {prompt.origen && <p className="ayuda copia-de">Copia de «{prompt.origen.titulo}»</p>}

      <div className="chips">
        {prompt.herramienta && <span className="chip">{prompt.herramienta}</span>}
        {prompt.carrera && <span className="chip">{prompt.carrera}</span>}
      </div>

      <div className="pie-autor">
        <div className="iniciales" aria-hidden="true">{iniciales(autor.nombre)}</div>
        <div className="quien">
          <strong>{autor.nombre ?? 'Alguien de la comunidad'}</strong>
          <span>{autor.carrera ?? ''}</span>
        </div>
        <PromedioEstrellas promedio={prompt.promedio} valoraciones={prompt.valoraciones} />
      </div>

      <div className="acciones">
        <button type="button" className="principal crece" onClick={soloEsto(alCopiar)}>Copiar prompt</button>
        <button type="button" className="secundario crece" onClick={soloEsto(alAbrir)}>Ver detalle</button>
      </div>
    </article>
  )
}
