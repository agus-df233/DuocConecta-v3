// Formas grises mientras carga: la página no salta cuando llegan los datos, y se entiende que algo
// viene en camino. El texto para lectores de pantalla va en aria-label.

/** Tarjetas vacías, para grillas como la vitrina o la biblioteca. */
export function EsqueletoTarjetas({ cantidad = 6, texto = 'Cargando' }) {
  return (
    <div className="grilla" aria-busy="true" aria-label={texto}>
      {Array.from({ length: cantidad }, (_, n) => (
        <div key={n} className="tarjeta esqueleto" aria-hidden="true">
          <span className="linea corta" />
          <span className="linea titulo" />
          <span className="linea" />
          <span className="linea" />
          <span className="linea media" />
        </div>
      ))}
    </div>
  )
}

/** Filas vacías, para tablas y listas de Administración o el perfil. */
export function EsqueletoLista({ filas = 5, texto = 'Cargando' }) {
  return (
    <div className="tarjeta esqueleto lista-esqueleto" aria-busy="true" aria-label={texto}>
      {Array.from({ length: filas }, (_, n) => (
        <span key={n} className={n === 0 ? 'linea titulo' : n % 3 === 2 ? 'linea media' : 'linea'} aria-hidden="true" />
      ))}
    </div>
  )
}
