// Piezas del dashboard de administración: una cifra grande y barras horizontales hechas con CSS,
// sin librerías de gráficos.

/** Un total con su rótulo; en rojo si pide atención. */
export function Cifra({ valor, texto, detalle, alerta = false }) {
  return (
    <div className={alerta ? 'cifra alerta' : 'cifra'}>
      <strong>{valor}</strong>
      <span>{texto}</span>
      {detalle && <small>{detalle}</small>}
    </div>
  )
}

/** Barras horizontales hechas con CSS: el ancho es proporcional a la categoría más grande. */
export function Barras({ titulo, conteos = [], etiquetas = {}, ayuda }) {
  const maximo = Math.max(1, ...conteos.map((c) => c.cantidad))
  return (
    <div className="tarjeta barras">
      <h2>{titulo}</h2>
      {ayuda && <p className="ayuda">{ayuda}</p>}
      {conteos.length === 0 ? (
        <p className="ayuda">Todavía no hay datos.</p>
      ) : (
        <ul>
          {conteos.map((c) => (
            <li key={c.clave}>
              <span className="etiqueta" title={etiquetas[c.clave] ?? c.clave}>{etiquetas[c.clave] ?? c.clave}</span>
              <span className="riel-barra">
                <span className="relleno" style={{ '--proporcion': c.cantidad / maximo }} />
              </span>
              <span className="numero">{c.cantidad}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
