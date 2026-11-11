// Cliente de la API. Cada petición lleva el token del proveedor con que se entró (Microsoft o Google).

import { obtenerToken } from './auth'
import { conCache } from './cache'

// Vacío = mismo origen, que es como queda detrás del API Gateway: llamadas relativas y sin CORS.
// En local lleva la URL completa, porque el front está en el 5173 y el BFF en el 8080.
const BASE = import.meta.env.VITE_BFF_URL ?? ''

/** Hace una petición al BFF adjuntando el token y traduciendo los errores a algo legible. */
async function pedir(ruta, opciones = {}) {
  const token = await obtenerToken()

  const respuesta = await fetch(`${BASE}${ruta}`, {
    ...opciones,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...opciones.headers,
    },
  })

  if (respuesta.status === 401) {
    throw new Error('Tu sesión no es válida o expiró. Vuelve a iniciar sesión.')
  }
  if (respuesta.status === 403) {
    // El backend responde 403 cuando el correo no pertenece a un dominio institucional,
    // y también cuando la acción es de otra persona.
    const problema = await respuesta.json().catch(() => ({}))
    throw new Error(problema.detail ?? problema.mensaje ?? 'No tienes permiso para esta operación.')
  }
  if (respuesta.status === 503) {
    throw new Error('El servicio no está respondiendo. Puede que el backend esté apagado.')
  }
  if (!respuesta.ok) {
    const problema = await respuesta.json().catch(() => ({}))
    throw new Error(problema.detail ?? problema.mensaje ?? `El servidor respondió ${respuesta.status}.`)
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

// --- Perfil (ms-usuarios, vía BFF) --------------------------------------------

/** Perfil y redes del usuario autenticado, en una sola llamada agregada por el BFF. */
export const obtenerMiPerfil = () => conCache('mi-perfil', () => pedir('/api/v1/bff/mi-perfil'))

/** Guarda los cambios del perfil propio. */
export const guardarMiPerfil = (perfil) =>
  pedir('/api/v1/usuarios/me', { method: 'PUT', body: JSON.stringify(perfil) })

/** Muestra u oculta el perfil propio en las búsquedas. */
export const alternarVisibilidad = () =>
  pedir('/api/v1/usuarios/me/visibilidad', { method: 'PATCH' })

// --- Vitrina de proyectos -----------------------------------------------------
//
// Listar y comentar van por el BFF, que suma quién publicó y quién comentó. Publicar, editar y
// borrar van directo a ms-proyectos: ahí no hay nada que componer.

/** Proyectos que la persona puede ver, con los datos de quien publicó cada uno. */
export const listarProyectos = () => conCache('vitrina', () => pedir('/api/v1/bff/vitrina'))

/** Publica un proyecto nuevo. */
export const publicarProyecto = (proyecto) =>
  pedir('/api/v1/proyectos', { method: 'POST', body: JSON.stringify(proyecto) })

/** Edita un proyecto propio. Cambiar la visibilidad a PRIVADO es la forma de ocultarlo. */
export const editarProyecto = (id, proyecto) =>
  pedir(`/api/v1/proyectos/${id}`, { method: 'PUT', body: JSON.stringify(proyecto) })

/**
 * Publica un proyecto abierto (solo ADMINISTRADOR): sale como "Publicado por DuocConecta", se puede
 * comentar y no recibe solicitudes de colaboración.
 */
export const publicarProyectoAbierto = (proyecto) =>
  pedir('/api/v1/proyectos/admin/abiertos', { method: 'POST', body: JSON.stringify(proyecto) })

/** Edita un proyecto abierto (solo ADMINISTRADOR). */
export const editarProyectoAbierto = (id, proyecto) =>
  pedir(`/api/v1/proyectos/admin/abiertos/${id}`, { method: 'PUT', body: JSON.stringify(proyecto) })

/** Pide autorización para subir un archivo. Devuelve la clave y la URL firmada. */
export const firmarAdjunto = (archivo) =>
  pedir('/api/v1/proyectos/adjuntos/firma', {
    method: 'POST',
    body: JSON.stringify({
      nombre: archivo.name,
      // Algunos navegadores dejan el tipo vacío; S3 exige que el PUT use exactamente el que se firma.
      tipoContenido: archivo.type || 'application/octet-stream',
      tamanoBytes: archivo.size,
    }),
  })

/**
 * Sube el archivo directo a S3 con la URL firmada. No pasa por el backend.
 *
 * <p>Usa XMLHttpRequest y no fetch porque es la única forma de informar el progreso. El
 * Content-Type tiene que ser el mismo que se firmó, o S3 rechaza la firma.</p>
 */
export function subirArchivoAS3(archivo, firma, alAvanzar) {
  return new Promise((resolver, rechazar) => {
    const peticion = new XMLHttpRequest()
    peticion.open('PUT', firma.urlFirmada)
    peticion.setRequestHeader('Content-Type', archivo.type || 'application/octet-stream')

    peticion.upload.onprogress = (e) => {
      if (e.lengthComputable && alAvanzar) alAvanzar(Math.round((e.loaded / e.total) * 100))
    }
    peticion.onload = () => (peticion.status >= 200 && peticion.status < 300
      ? resolver()
      : rechazar(new Error(`S3 rechazó la subida (${peticion.status}).`)))
    peticion.onerror = () => rechazar(new Error('No se pudo conectar con el almacenamiento.'))

    peticion.send(archivo)
  })
}

/** Firma, sube y devuelve el adjunto listo para mandar con el proyecto. */
export async function subirAdjunto(archivo, alAvanzar) {
  const firma = await firmarAdjunto(archivo)
  await subirArchivoAS3(archivo, firma, alAvanzar)
  return {
    claveS3: firma.clave,
    nombre: archivo.name,
    tipoContenido: archivo.type || 'application/octet-stream',
    tamanoBytes: archivo.size,
  }
}

/** Borra un proyecto propio de forma definitiva. */
export const eliminarProyecto = (id) =>
  pedir(`/api/v1/proyectos/${id}`, { method: 'DELETE' })

/** Hilo de comentarios de un proyecto. */
export const listarComentarios = (id) => pedir(`/api/v1/bff/vitrina/${id}/comentarios`)

/** Deja un comentario. Comentar no comparte ningún dato de contacto. */
export const comentarProyecto = (id, texto) =>
  pedir(`/api/v1/bff/vitrina/${id}/comentarios`, { method: 'POST', body: JSON.stringify({ texto }) })

// --- Solicitudes de colaboración (ms-contacto, vía BFF) ------------------------
//
// Van por el BFF porque al aceptar compone los datos con el perfil de quien acepta: así nadie
// tiene que escribir su correo a mano.

/** Le pide contacto a la persona dueña de un proyecto. */
export const solicitarColaboracion = (solicitud) =>
  pedir('/api/v1/bff/colaboraciones', { method: 'POST', body: JSON.stringify(solicitud) })

/** Acepta o rechaza una solicitud recibida. */
export const responderColaboracion = (id, aceptar, compartirTelefono = false) =>
  pedir(`/api/v1/bff/colaboraciones/${id}/responder`, {
    method: 'PATCH',
    body: JSON.stringify({ aceptar, compartirTelefono }),
  })

/** Retira una solicitud que envié y que nadie ha respondido todavía. */
export const retirarColaboracion = (id) =>
  pedir(`/api/v1/bff/colaboraciones/${id}/retirar`, { method: 'PATCH' })

/** Solicitudes que otras personas me enviaron. */
export const colaboracionesRecibidas = () => pedir('/api/v1/bff/colaboraciones/recibidas')

/** Solicitudes que envié, con los datos de contacto de las aceptadas. */
export const colaboracionesEnviadas = () => pedir('/api/v1/bff/colaboraciones/enviadas')

// --- Notificaciones (ms-notificaciones) ---------------------------------------
//
// Van directo por el API Gateway, como publicar proyectos: el aviso ya viene armado por el
// consumidor y no hay nada que componer en el BFF.

/** Mis avisos, lo más nuevo primero. */
export const listarAvisos = () => pedir('/api/v1/notificaciones')

/** Marca un aviso propio como leído. */
export const marcarAvisoLeido = (id) =>
  pedir(`/api/v1/notificaciones/${id}/leido`, { method: 'PATCH' })

// --- Administración (solo ADMINISTRADOR) --------------------------------------
//
// El resumen va por el BFF porque cruza ms-usuarios con ms-proyectos (proyectos por carrera).
// Moderar va directo a ms-proyectos, igual que publicar: no hay nada que componer.

/** Cifras de la comunidad y de la vitrina para el dashboard. */
export const resumenAdministracion = () => pedir('/api/v1/bff/admin/resumen')

/** Todos los proyectos, incluidos privados y ocultos. Los filtros son opcionales. */
export const proyectosParaModerar = ({ oculto, visibilidad } = {}) => {
  const filtros = new URLSearchParams()
  if (oculto !== undefined && oculto !== '') filtros.set('oculto', oculto)
  if (visibilidad) filtros.set('visibilidad', visibilidad)
  const consulta = filtros.toString()
  return pedir(`/api/v1/proyectos/admin${consulta ? `?${consulta}` : ''}`)
}

/** Saca un proyecto de la vitrina. La dueña recibe un aviso con el motivo. */
export const ocultarProyecto = (id, motivo) =>
  pedir(`/api/v1/proyectos/admin/${id}/ocultar`, { method: 'PATCH', body: JSON.stringify({ motivo }) })

/** Devuelve a la vitrina un proyecto oculto por moderación. */
export const mostrarProyecto = (id) => pedir(`/api/v1/proyectos/admin/${id}/mostrar`, { method: 'PATCH' })

/** Borra cualquier proyecto, sin la regla de dueña. */
export const eliminarProyectoModerado = (id) => pedir(`/api/v1/proyectos/admin/${id}`, { method: 'DELETE' })

/** Los 50 comentarios más recientes de toda la plataforma. */
export const comentariosParaModerar = () => pedir('/api/v1/proyectos/admin/comentarios')

/** Borra un comentario. Quien lo escribió recibe un aviso. */
export const eliminarComentarioModerado = (id) =>
  pedir(`/api/v1/proyectos/admin/comentarios/${id}`, { method: 'DELETE' })

// --- Streaming (v3): Kafka -----------------------------------------------------
//
// Abrir un proyecto es una señal que va solo a Kafka. La tendencia la arma ms-analitica, que
// consume los tópicos: si está caído, la vitrina sigue igual, solo sin la marca "En tendencia".

/** Anota que se abrió el detalle de un proyecto. Las vistas de la dueña no cuentan. */
export const registrarVistaProyecto = (id) =>
  pedir(`/api/v1/proyectos/${id}/vistas`, { method: 'POST' })

/** Lo que está en tendencia: identificadores y puntaje de los últimos 7 días. */
export const tendencias = (tipoRecurso = 'PROYECTO', limite = 20) =>
  pedir(`/api/v1/analitica/tendencias?tipoRecurso=${tipoRecurso}&limite=${limite}`)

/** Cuántas interacciones tuvo un proyecto o un prompt esta semana. */
export const interaccionesDe = (id) => pedir(`/api/v1/analitica/recursos/${id}`)

// --- Administración de Kafka (solo ADMINISTRADOR) --------------------------------

/** Alertas de actividad que detectó el consumidor, con el nombre de cada persona (BFF). */
export const alertasActividad = (pendientes = false) =>
  pedir(`/api/v1/bff/admin/alertas?pendientes=${pendientes}`)

/** Marca una alerta como revisada. */
export const marcarAlertaRevisada = (id) =>
  pedir(`/api/v1/analitica/admin/alertas/${id}/revisada`, { method: 'PATCH' })

/** Registros por hora de los dos tópicos. */
export const metricasActividad = (horas = 24) => pedir(`/api/v1/analitica/admin/metricas?horas=${horas}`)

/** Estado de los consumidores de Kafka de ms-analitica. */
export const consumidoresKafka = () => pedir('/api/v1/analitica/admin/consumidores')

/** Pausa los consumidores: siguen en su grupo pero no leen, y el lag sube. */
export const pausarConsumidoresKafka = () =>
  pedir('/api/v1/analitica/admin/consumidores/pausa', { method: 'POST' })

/** Los reanuda: siguen desde su último offset confirmado. */
export const reanudarConsumidoresKafka = () =>
  pedir('/api/v1/analitica/admin/consumidores/reanudar', { method: 'POST' })

/** Brokers vivos y controlador (ms-mensajeria). */
export const clusterKafka = () => pedir('/api/v1/mensajeria/kafka/cluster')

/** Tópicos de la plataforma con su configuración. */
export const topicosKafka = () => pedir('/api/v1/mensajeria/kafka/topicos')

/** Grupos de consumidores con su lag total. */
export const gruposKafka = () => pedir('/api/v1/mensajeria/kafka/grupos')

/** Un grupo: quién atiende cada partición, offsets y lag. */
export const grupoKafka = (id) => pedir(`/api/v1/mensajeria/kafka/grupos/${encodeURIComponent(id)}`)

// --- Biblioteca de prompts (v3) ------------------------------------------------
//
// Buscar y ver el detalle van por el BFF, que suma el nombre de cada autor. Publicar, editar,
// bifurcar y valorar van directo a ms-prompts: ahí no hay nada que componer.

/** Busca en la biblioteca. Los filtros vacíos no se envían. */
export const listarPrompts = (filtros = {}) => {
  const parametros = new URLSearchParams(Object.entries(filtros).filter(([, v]) => v))
  const ruta = `/api/v1/bff/biblioteca${parametros.size ? `?${parametros}` : ''}`
  return conCache(`biblioteca:${parametros}`, () => pedir(ruta))
}

/** El detalle de un prompt, con su autor y, si es una copia, el del original. */
export const obtenerPrompt = (id) => pedir(`/api/v1/bff/biblioteca/${id}`)

/** Los prompts propios, también los privados. */
export const misPrompts = () => conCache('mis-prompts', () => pedir('/api/v1/prompts/mios'))

/** Publica un prompt. */
export const publicarPrompt = (prompt) =>
  pedir('/api/v1/prompts', { method: 'POST', body: JSON.stringify(prompt) })

/** Edita un prompt propio. */
export const editarPrompt = (id, prompt) =>
  pedir(`/api/v1/prompts/${id}`, { method: 'PUT', body: JSON.stringify(prompt) })

/** Borra un prompt propio. Las copias que hicieron otras personas siguen. */
export const eliminarPrompt = (id) => pedir(`/api/v1/prompts/${id}`, { method: 'DELETE' })

/** Hace una copia propia y privada del prompt, que recuerda de dónde salió. */
export const bifurcarPrompt = (id) => pedir(`/api/v1/prompts/${id}/bifurcaciones`, { method: 'POST' })

/** Valora con 1 a 5 estrellas. Valorar de nuevo reemplaza. */
export const valorarPrompt = (id, estrellas) =>
  pedir(`/api/v1/prompts/${id}/valoracion`, { method: 'PUT', body: JSON.stringify({ estrellas }) })

/** Se abrió el detalle: señal para la tendencia (solo Kafka). */
export const registrarVistaPrompt = (id) => pedir(`/api/v1/prompts/${id}/vistas`, { method: 'POST' })

/** Se copió el texto: la señal de uso más fuerte (solo Kafka). */
export const registrarCopiaPrompt = (id) => pedir(`/api/v1/prompts/${id}/copias`, { method: 'POST' })

// Moderación de la biblioteca (solo ADMINISTRADOR).

/** Todos los prompts; `oculto` vacío, 'true' o 'false'. */
export const promptsParaModerar = (oculto = '') =>
  pedir(`/api/v1/prompts/admin${oculto ? `?oculto=${oculto}` : ''}`)

/** Saca un prompt de la biblioteca con un motivo. */
export const ocultarPrompt = (id, motivo) =>
  pedir(`/api/v1/prompts/admin/${id}/ocultar`, { method: 'PATCH', body: JSON.stringify({ motivo }) })

/** Lo devuelve a la biblioteca. */
export const mostrarPrompt = (id) => pedir(`/api/v1/prompts/admin/${id}/mostrar`, { method: 'PATCH' })

/** Lo borra. */
export const eliminarPromptModerado = (id) => pedir(`/api/v1/prompts/admin/${id}`, { method: 'DELETE' })

/** Cifras de la biblioteca. */
export const estadisticasPrompts = () => pedir('/api/v1/prompts/admin/estadisticas')

// --- Administración (v3): personas, Kafka y mensajería ---------------------------

const json = (cuerpo) => ({ body: JSON.stringify(cuerpo) })

/** Busca personas en toda la comunidad (también ocultas). Sin teléfono ni redes. */
export const buscarPersonas = (filtros = {}) => {
  const parametros = new URLSearchParams(Object.entries(filtros).filter(([, v]) => v !== '' && v != null))
  return pedir(`/api/v1/usuarios/admin/personas?${parametros}`)
}

/** Ficha de una persona: perfil, proyectos, prompts y alertas (BFF). */
export const fichaPersona = (oid) => pedir(`/api/v1/bff/admin/personas/${encodeURIComponent(oid)}`)

/** Saca un perfil de los listados públicos por moderación. */
export const ocultarPersona = (id, motivo) =>
  pedir(`/api/v1/usuarios/admin/personas/${id}/ocultar`, { method: 'PATCH', ...json({ motivo }) })

/** Lo devuelve a los listados. */
export const mostrarPersona = (id) => pedir(`/api/v1/usuarios/admin/personas/${id}/mostrar`, { method: 'PATCH' })

/** Detalle de un tópico: líder, réplicas, ISR y offsets por partición. */
export const topicoKafka = (nombre) => pedir(`/api/v1/mensajeria/kafka/topicos/${encodeURIComponent(nombre)}`)

/** Crea un tópico. */
export const crearTopicoKafka = (datos) => pedir('/api/v1/mensajeria/kafka/topicos', { method: 'POST', ...json(datos) })

/** Cambia la configuración de un tópico (por ejemplo, la retención). */
export const configurarTopicoKafka = (nombre, fijar) =>
  pedir(`/api/v1/mensajeria/kafka/topicos/${encodeURIComponent(nombre)}/configuracion`, { method: 'PATCH', ...json({ fijar }) })

/** Aumenta las particiones de un tópico. */
export const aumentarParticionesKafka = (nombre, total) =>
  pedir(`/api/v1/mensajeria/kafka/topicos/${encodeURIComponent(nombre)}/particiones`, { method: 'POST', ...json({ total }) })

/** Borra un tópico. */
export const eliminarTopicoKafka = (nombre) =>
  pedir(`/api/v1/mensajeria/kafka/topicos/${encodeURIComponent(nombre)}`, { method: 'DELETE' })

/** Mueve los offsets de un grupo vacío: INICIO, FIN o FECHA. */
export const moverOffsetsKafka = (grupo, datos) =>
  pedir(`/api/v1/mensajeria/kafka/grupos/${encodeURIComponent(grupo)}/offsets`, { method: 'POST', ...json(datos) })

/** Colas SQS de la plataforma. */
export const colasSqs = () => pedir('/api/v1/mensajeria/colas')
export const purgarColaSqs = (nombre) => pedir(`/api/v1/mensajeria/colas/${nombre}/purga`, { method: 'POST' })
export const redriveSqs = (nombre) => pedir(`/api/v1/mensajeria/colas/${nombre}/redrive`, { method: 'POST' })

/** Colas de RabbitMQ (la topología base) y sus operaciones. */
export const colasRabbit = () => pedir('/api/v1/mensajeria/rabbit/colas')
export const crearColaRabbit = (datos) => pedir('/api/v1/mensajeria/rabbit/colas', { method: 'POST', ...json(datos) })
export const eliminarColaRabbit = (nombre) => pedir(`/api/v1/mensajeria/rabbit/colas/${nombre}`, { method: 'DELETE' })
export const purgarColaRabbit = (nombre) => pedir(`/api/v1/mensajeria/rabbit/colas/${nombre}/purga`, { method: 'POST' })
export const redriveRabbit = (nombre) => pedir(`/api/v1/mensajeria/rabbit/colas/${nombre}/redrive`, { method: 'POST' })
export const crearBindingRabbit = (datos) => pedir('/api/v1/mensajeria/rabbit/bindings', { method: 'POST', ...json(datos) })
export const eliminarBindingRabbit = (datos) => pedir('/api/v1/mensajeria/rabbit/bindings', { method: 'DELETE', ...json(datos) })

/** Las DLQ de los dos brokers y sus mensajes (ms-notificaciones). */
export const descartes = () => pedir('/api/v1/notificaciones/admin/descartes')
export const mensajesDescartados = (cola) => pedir(`/api/v1/notificaciones/admin/descartes/${cola}/mensajes`)

/** Consumidores de SQS y RabbitMQ de ms-notificaciones: estado, pausa y reanudación. */
export const consumidoresMensajeria = () => pedir('/api/v1/notificaciones/admin/consumidores')
export const pausarConsumidoresMensajeria = () => pedir('/api/v1/notificaciones/admin/consumidores/pausa', { method: 'POST' })
export const reanudarConsumidoresMensajeria = () => pedir('/api/v1/notificaciones/admin/consumidores/reanudar', { method: 'POST' })
