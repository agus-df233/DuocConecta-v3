// Tabla de Administración. En el celular cada fila se muestra como tarjeta, y cada celda necesita
// el nombre de su columna: se copia del encabezado a data-etiqueta después de cada render, así las
// tablas se escriben como siempre.
import { useLayoutEffect, useRef } from 'react'

export default function TablaAdmin({ children }) {
  const tabla = useRef(null)

  useLayoutEffect(() => {
    const titulos = [...(tabla.current?.querySelectorAll('thead th') ?? [])].map((th) => th.textContent.trim())
    tabla.current?.querySelectorAll('tbody tr').forEach((fila) => {
      [...fila.children].forEach((celda, i) => {
        if (titulos[i]) celda.dataset.etiqueta = titulos[i]
        else delete celda.dataset.etiqueta
      })
    })
  })

  return <table ref={tabla} className="tabla-admin">{children}</table>
}
