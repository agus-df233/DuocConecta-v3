// Navegación de abajo en el celular: las vistas al alcance del pulgar, con ícono y texto. En
// pantallas anchas no se muestra (la cabecera ya tiene la navegación).

const ICONOS = {
  vitrina: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  biblioteca: 'M6 3h9l3 3v15H6zM9 10h6M9 14h6M9 18h4',
  perfil: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c0-4 4-6 8-6s8 2 8 6',
  admin: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z',
}

export default function BarraInferior({ vista, irA, esAdministrador }) {
  const opciones = [
    ['vitrina', 'Vitrina'],
    ['biblioteca', 'Prompts'],
    ['perfil', 'Perfil'],
    ...(esAdministrador ? [['admin', 'Admin']] : []),
  ]
  return (
    <nav className="barra-inferior" aria-label="Navegación principal" style={{ '--opciones': opciones.length }}>
      {opciones.map(([id, texto]) => (
        <button key={id} type="button" className={vista === id ? 'activo' : ''}
                aria-current={vista === id ? 'page' : undefined} onClick={() => irA(id)}>
          <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true" fill="none" stroke="currentColor"
               strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <path d={ICONOS[id]} />
          </svg>
          <span>{texto}</span>
        </button>
      ))}
    </nav>
  )
}
