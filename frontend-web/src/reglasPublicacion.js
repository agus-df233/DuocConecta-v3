// Recomendaciones antes de publicar: funciones puras, sin React. Cada regla mira los datos del
// formulario y dice si se cumple, con un texto corto para el checklist y un consejo de formato
// para cuando se enfoca el campo. No bloquean: publicar con pendientes igual se puede.

const largo = (texto) => (texto ?? '').trim().length

/** Las recomendaciones de un proyecto, en el orden en que se llena el formulario. */
export function recomendacionesProyecto(d) {
  const detalle = d.descripcion ?? ''
  const reglas = [
    { id: 'nombre', campo: 'nombre', texto: 'Un nombre que diga qué es',
      cumple: largo(d.nombre) >= 8 && d.nombre !== d.nombre?.toUpperCase() },
    { id: 'resumen', campo: 'resumen', texto: 'Un resumen de 60 a 200 caracteres',
      cumple: largo(d.resumen) >= 60 && largo(d.resumen) <= 200 },
    { id: 'detalle', campo: 'descripcion', texto: 'Un detalle con qué resuelve, en qué etapa va y qué ayuda buscas',
      cumple: largo(detalle) >= 150 },
    { id: 'herramientas', campo: 'herramientas', texto: 'Al menos 2 herramientas',
      cumple: (d.herramientas ?? []).length >= 2 },
    { id: 'enlace', campo: 'urlRepositorio', texto: 'Un enlace al repositorio o a una demo',
      cumple: /^https?:\/\/\S+\.\S+/.test(d.urlRepositorio ?? '') },
    { id: 'adjunto', campo: 'adjuntos', texto: 'Una captura o un documento adjunto',
      cumple: (d.adjuntos ?? []).length >= 1 },
  ]
  if (d.estado === 'BUSCANDO_EQUIPO') {
    reglas.push({ id: 'perfil', campo: 'descripcion', texto: 'Qué perfil buscas para el equipo',
      cumple: /(busc|necesit|falta|perfil|alguien de|me sumo|sumar)/i.test(detalle) })
  }
  return reglas
}

/** Las recomendaciones de un prompt: lo que hace que otra persona lo pueda reutilizar. */
export function recomendacionesPrompt(d) {
  const texto = d.contenido ?? ''
  return [
    { id: 'proposito', campo: 'proposito', texto: 'Para qué sirve, en una línea clara', cumple: largo(d.proposito) >= 40 },
    { id: 'rol', campo: 'contenido', texto: 'Un rol ("Actúa como…", "Eres…")',
      cumple: /(act[uú]a como|eres (un|una|el|la)|como (un|una) experto|tu rol)/i.test(texto) },
    { id: 'contexto', campo: 'contenido', texto: 'El contexto de la tarea', cumple: largo(texto) >= 120 },
    { id: 'formato', campo: 'contenido', texto: 'El formato de salida esperado',
      cumple: /(formato|tabla|lista|pasos|vi[ñn]etas|json|markdown|dame:|responde (con|en)|estructura|en \d+ )/i.test(texto) },
    { id: 'corchetes', campo: 'contenido', texto: '[Corchetes] en lo que hay que completar', cumple: /\[[^\]]+\]/.test(texto) },
    { id: 'herramienta', campo: 'herramienta', texto: 'Con qué herramienta lo probaste', cumple: largo(d.herramienta) > 0 },
  ]
}

/** Consejo de formato que aparece al enfocar cada campo. */
export const CONSEJOS = {
  nombre: 'Un nombre corto y concreto: "Reserva de salas por sede" dice más que "Proyecto final".',
  resumen: 'Es lo único que se lee en la tarjeta: qué hace y para quién, en una o dos frases.',
  descripcion: 'Cuenta qué problema resuelve, en qué etapa está y qué ayuda necesitas. Si buscas equipo, di qué perfil.',
  herramientas: 'Sepáralas con comas. Sirven para que te encuentren: Figma, Excel o React cuentan igual.',
  urlRepositorio: 'Un repositorio, un Figma o una demo: quien mira quiere ver el trabajo.',
  proposito: 'En una línea: qué problema resuelve el prompt. "Explica un error sin darte la solución".',
  contenido: 'Dale un rol, el contexto, el formato de salida y marca con [corchetes] lo que hay que completar.',
  herramienta: 'Con qué IA lo probaste: el mismo prompt no rinde igual en todas.',
}

/** Los consejos del carrusel de la primera vez. */
export const PRIMERA_VEZ = {
  proyecto: [
    { titulo: 'Un nombre que se entienda solo', texto: 'Quien recorre la vitrina decide en un segundo. Di qué es, no cómo se llama la asignatura.' },
    { titulo: 'El resumen vende la tarjeta', texto: 'Entre 60 y 200 caracteres: qué hace y para quién. El detalle completo va abajo.' },
    { titulo: 'Muestra el trabajo', texto: 'Un enlace y una captura hacen que te pidan colaborar. Si buscas equipo, di qué perfil necesitas.' },
  ],
  prompt: [
    { titulo: 'Dale un rol', texto: '"Actúa como profesor de programación" orienta a la IA mejor que una orden suelta.' },
    { titulo: 'Contexto y formato', texto: 'Cuenta la situación y pide la salida en un formato: pasos, una tabla, cinco preguntas.' },
    { titulo: 'Hazlo reutilizable', texto: 'Marca con [corchetes] lo que cada persona completa, y di con qué herramienta lo probaste.' },
  ],
}

const CLAVE_VISTOS = 'duocconecta.consejos-vistos'

/** Si ya se mostró el carrusel de la primera vez para ese tipo. */
export function consejosVistos(tipo) {
  try {
    return JSON.parse(window.localStorage.getItem(CLAVE_VISTOS) ?? '[]').includes(tipo)
  } catch {
    return false
  }
}

/** Anota que no hay que volver a mostrarlo. */
export function marcarConsejosVistos(tipo) {
  try {
    const vistos = new Set(JSON.parse(window.localStorage.getItem(CLAVE_VISTOS) ?? '[]'))
    vistos.add(tipo)
    window.localStorage.setItem(CLAVE_VISTOS, JSON.stringify([...vistos]))
  } catch {
    // Sin almacenamiento: se vuelve a mostrar la próxima vez, no es grave.
  }
}
