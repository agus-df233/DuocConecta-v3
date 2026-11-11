// Ventana para compartir un proyecto o un prompt. Arriba, lo rápido: el menú nativo del celular
// y los botones de WhatsApp, LinkedIn, X, Telegram y correo. En un proyecto, además, Instagram,
// WhatsApp y GitHub con una vista previa de cómo se verá. Nada se publica solo: se abre la red con
// el mensaje listo, y la persona decide. El enlace exige iniciar sesión: no expone nada sin login.
import { useState } from 'react'
import { CATEGORIAS_PROMPT, ESTADOS, iniciales } from './catalogo'
import { enlaceDePrompt, enlaceDeProyecto } from './enlaces'
import Ventana from './Ventana'

/** El menú nativo de compartir solo tiene sentido en pantallas táctiles: en el computador se esconde. */
const hayMenuNativo = () => typeof navigator.share === 'function'
  && Boolean(window.matchMedia?.('(pointer: coarse)').matches)

/** El texto corto y el enlace de un proyecto o de un prompt. */
function contenidoDe(proyecto, prompt) {
  if (prompt) {
    const autor = prompt.autor?.nombre?.split(' ')[0]
    return {
      titulo: prompt.titulo,
      texto: `Este prompt me sirvió: «${prompt.titulo}»${autor ? ` (de ${autor})` : ''}. Está en la biblioteca de DuocConecta:`,
      url: enlaceDePrompt(prompt.id),
      etiqueta: CATEGORIAS_PROMPT[prompt.categoria] ?? 'Prompt',
    }
  }
  const primer = proyecto.autor?.nombre?.split(' ')[0] ?? 'alguien de la comunidad'
  return {
    titulo: proyecto.nombre,
    texto: proyecto.estado === 'BUSCANDO_EQUIPO'
      ? `${primer} está buscando equipo para «${proyecto.nombre}». Si te interesa, pide colaborar en DuocConecta:`
      : `Mira «${proyecto.nombre}», proyecto de ${primer} en DuocConecta:`,
    url: enlaceDeProyecto(proyecto.id),
    etiqueta: ESTADOS[proyecto.estado] ?? 'Proyecto',
  }
}

/** Las redes con el mensaje listo. Cada una se abre en otra pestaña con su propio formulario. */
function redes({ titulo, texto, url }) {
  const t = encodeURIComponent(texto)
  const u = encodeURIComponent(url)
  return [
    { id: 'whatsapp', red: 'WhatsApp', href: `https://wa.me/?text=${encodeURIComponent(`${texto} ${url}`)}` },
    { id: 'linkedin', red: 'LinkedIn', href: `https://www.linkedin.com/sharing/share-offsite/?url=${u}` },
    { id: 'x', red: 'X', href: `https://x.com/intent/post?text=${t}&url=${u}` },
    { id: 'telegram', red: 'Telegram', href: `https://t.me/share/url?url=${u}&text=${t}` },
    { id: 'correo', red: 'Correo', href: `mailto:?subject=${encodeURIComponent(`${titulo} · DuocConecta`)}&body=${encodeURIComponent(`${texto}\n\n${url}`)}` },
  ]
}

/** Copia al portapapeles. El respaldo con textarea cubre los navegadores sin clipboard API. */
async function copiar(texto) {
  try {
    await navigator.clipboard.writeText(texto)
  } catch {
    const area = document.createElement('textarea')
    area.value = texto
    area.setAttribute('readonly', '')
    area.className = 'fuera-de-pantalla'
    document.body.appendChild(area)
    area.select()
    document.execCommand('copy')
    area.remove()
  }
}

/** Los textos de cada red, armados con los datos reales del proyecto. */
function canales(proyecto) {
  const autor = proyecto.autor ?? {}
  const primer = autor.nombre?.split(' ')[0] ?? 'alguien de la comunidad'
  const estado = (ESTADOS[proyecto.estado] ?? '').toLowerCase()
  const busca = proyecto.estado === 'BUSCANDO_EQUIPO'
  const url = enlaceDeProyecto(proyecto.id)
  const carrera = autor.carrera ? ` (${autor.carrera})` : ''
  const etiqueta = autor.carrera ? ` #${autor.carrera.replace(/\s+/g, '')}` : ''
  const insignia = `[![DuocConecta](https://img.shields.io/badge/DuocConecta-${encodeURIComponent(estado).replace(/-/g, '--')}-1B4DB1)](${url})`
  const whatsapp = busca
    ? `${primer} está buscando equipo para «${proyecto.nombre}». Si te interesa, pide colaborar acá:`
    : `Mira «${proyecto.nombre}», proyecto de ${primer} en DuocConecta:`

  return {
    instagram: {
      red: 'Instagram', sigla: 'IG', titulo: 'Historia o publicación',
      descripcion: 'Instagram no deja publicar desde la web. Copia el texto y súbelo desde la app etiquetando a @duocuc.',
      rotulo: 'Texto para la descripción',
      texto: `${proyecto.nombre} ✦ ${estado}. Proyecto de ${primer}${carrera}. ${busca ? '¿Te sumas? ' : ''}Míralo en DuocConecta.\n\n@duocuc #DuocConecta${etiqueta}`,
      href: 'https://www.instagram.com/', accion: 'Abrir Instagram', copiado: 'Texto copiado', copiarTexto: 'Copiar texto',
    },
    whatsapp: {
      red: 'WhatsApp', sigla: 'W', titulo: 'Mensaje a un chat o grupo',
      descripcion: 'Se abre WhatsApp con el mensaje listo para elegir el chat.',
      rotulo: 'Mensaje',
      texto: `${whatsapp} ${url}`,
      href: `https://wa.me/?text=${encodeURIComponent(`${whatsapp} ${url}`)}`, accion: 'Enviar por WhatsApp',
      copiado: 'Mensaje copiado', copiarTexto: 'Copiar mensaje', mensaje: whatsapp,
    },
    github: {
      red: 'GitHub', sigla: 'GH', titulo: 'Insignia para tu README',
      descripcion: 'Pega la insignia en el README del repositorio: enlaza a la ficha del proyecto para que te encuentren.',
      rotulo: 'Markdown',
      texto: `${insignia}\n\n> Proyecto publicado en DuocConecta por ${autor.nombre ?? 'su autor'}.\n> ${busca ? 'Buscamos colaboradores: ' : 'Ficha del proyecto: '}${url}`,
      href: proyecto.urlRepositorio || 'https://github.com/', accion: proyecto.urlRepositorio ? 'Abrir el repositorio' : 'Abrir GitHub',
      copiado: 'Markdown copiado', copiarTexto: 'Copiar Markdown', estado,
    },
    url,
  }
}

/** `proyecto` o `prompt`: lo que se comparte. */
export default function Compartir({ proyecto, prompt, alCerrar, alAvisar }) {
  const contenido = contenidoDe(proyecto, prompt)
  const [copiado, setCopiado] = useState(null)

  const copiarYAvisar = async (texto, cual, mensaje) => {
    await copiar(texto)
    setCopiado(cual)
    alAvisar(mensaje)
  }

  const menuNativo = async () => {
    try {
      await navigator.share({ title: contenido.titulo, text: contenido.texto, url: contenido.url })
    } catch {
      // La persona cerró el menú: no es un error.
    }
  }

  return (
    <Ventana titulo={`Compartir «${contenido.titulo}»`} alCerrar={alCerrar} ancha enfocarCampo={false}>
      <div className="compartir-rapido">
        <p className="rotulo">Compartir rápido</p>
        <div className="botones-redes">
          {hayMenuNativo() && (
            <button type="button" className="boton-red-rapido nativo" onClick={menuNativo}>
              <span className="icono-red" aria-hidden="true">⤴</span>Compartir…
            </button>
          )}
          {redes(contenido).map((r) => (
            <a key={r.id} className={`boton-red-rapido ${r.id}`} href={r.href} target="_blank" rel="noopener noreferrer">
              <span className="icono-red" aria-hidden="true">{r.red.slice(0, r.id === 'x' ? 1 : 2)}</span>
              {r.red}<span className="solo-lector"> (se abre en otra pestaña)</span>
            </a>
          ))}
        </div>
        <div className="compartir-enlace">
          <span>{contenido.url}</span>
          <button type="button" className="enlace" onClick={() => copiarYAvisar(contenido.url, 'enlace', 'Enlace copiado.')}>
            {copiado === 'enlace' ? 'Enlace copiado' : 'Copiar enlace'}
          </button>
        </div>
        <p className="ayuda">Quien abra el enlace tendrá que iniciar sesión con su cuenta para verlo.</p>
      </div>
      {proyecto && <ConVistaPrevia proyecto={proyecto} copiarYAvisar={copiarYAvisar} copiado={copiado} setCopiado={setCopiado} />}
    </Ventana>
  )
}

/** Instagram, WhatsApp y GitHub con una vista previa: solo para proyectos. */
function ConVistaPrevia({ proyecto, copiarYAvisar, copiado, setCopiado }) {
  const [canal, setCanal] = useState('instagram')
  const todos = canales(proyecto)
  const actual = todos[canal]
  const autor = proyecto.autor ?? {}

  return (
    <>
      <p className="rotulo compartir-titulo-vista">Con vista previa</p>
      <div className="compartir">
        <div className="compartir-canales" role="tablist" aria-label="Elige dónde compartir">
          {['instagram', 'whatsapp', 'github'].map((k) => (
            <button
              key={k}
              type="button"
              role="tab"
              id={`canal-${k}`}
              aria-selected={canal === k}
              aria-controls="panel-compartir"
              className={canal === k ? 'canal activo' : 'canal'}
              onClick={() => { setCanal(k); setCopiado(null) }}
            >
              <span className={`canal-icono ${k}`} aria-hidden="true">{todos[k].sigla}</span>
              {todos[k].red}
            </button>
          ))}
        </div>

        <div id="panel-compartir" role="tabpanel" aria-labelledby={`canal-${canal}`} className="compartir-panel">
          <div className="compartir-vista">
            {canal === 'instagram' && (
              <div className="vista-historia" role="img" aria-label="Vista previa de la historia de Instagram">
                <div className="historia-barras"><span /><span /></div>
                <div className="historia-autor">
                  <span className="historia-avatar">{iniciales(autor.nombre)}</span>
                  <span>{(autor.nombre?.split(' ')[0] ?? 'duoc').toLowerCase()}.duoc</span>
                </div>
                <div className="historia-cuerpo">
                  <span className="insignia-nuevo">Proyecto en DuocConecta</span>
                  <p className="historia-titulo">{proyecto.nombre}</p>
                  <p className="historia-autor-texto">{[autor.nombre, autor.carrera].filter(Boolean).join(' · ')}</p>
                  <div className="chips">
                    {(proyecto.herramientas ?? []).slice(0, 3).map((h) => <span key={h} className="chip oscuro">{h}</span>)}
                  </div>
                  <p className="historia-etiqueta">@duocuc #DuocConecta</p>
                </div>
              </div>
            )}
            {canal === 'whatsapp' && (
              <div className="vista-whatsapp" role="img" aria-label="Vista previa del mensaje de WhatsApp">
                <div className="whatsapp-cabecera">Grupo de tu carrera</div>
                <div className="whatsapp-burbuja">
                  <div className="whatsapp-tarjeta">
                    <span className="logo" />
                    <div>
                      <p className="titulo">{proyecto.nombre}</p>
                      <p>{ESTADOS[proyecto.estado]} · {autor.carrera ?? 'DuocConecta'}</p>
                    </div>
                  </div>
                  <p>{actual.mensaje} <span className="whatsapp-enlace">{todos.url}</span></p>
                </div>
              </div>
            )}
            {canal === 'github' && (
              <div className="vista-github" role="img" aria-label="Vista previa en GitHub">
                <div className="github-archivo">README.md</div>
                <div className="github-cuerpo">
                  <p className="titulo">{proyecto.nombre}</p>
                  <span className="github-insignia"><span>DuocConecta</span><span>{actual.estado}</span></span>
                  <p>{proyecto.resumen}</p>
                </div>
              </div>
            )}
          </div>

          <div className="compartir-datos">
            <div>
              <h3>{actual.titulo}</h3>
              <p className="ayuda">{actual.descripcion}</p>
            </div>
            <label>
              {actual.rotulo}
              <textarea readOnly rows={5} value={actual.texto} className="texto-compartir" />
            </label>
            <a
              className={`boton-red ${canal}`}
              href={actual.href}
              target="_blank"
              rel="noopener noreferrer"
              onClick={() => { if (canal !== 'whatsapp') copiar(actual.texto) }}
            >
              {actual.accion}<span className="solo-lector"> (se abre en otra pestaña)</span>
            </a>
            <button type="button" className="secundario" onClick={() => copiarYAvisar(actual.texto, canal, 'Copiado al portapapeles.')}>
              {copiado === canal ? actual.copiado : actual.copiarTexto}
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
