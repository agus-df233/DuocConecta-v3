// Panel de accesibilidad de la cabecera: tamaño del texto, alto contraste, reducir movimiento y
// cuánto duran los avisos. Los cambios se aplican a toda la aplicación y quedan guardados en este
// navegador.
import { DURACIONES_PUSH } from './CentroNotificaciones'
import Interruptor from './Interruptor'

const TAMANOS = ['Normal', 'Grande', 'Muy grande']

export default function Accesibilidad({ preferencias, alCambiar, abierto, alAlternar }) {
  return (
    <div className="desplegable">
      <button type="button" className="boton-barra boton-accesibilidad" onClick={alAlternar} aria-expanded={abierto}
              aria-haspopup="dialog" aria-label="Accesibilidad">
        {/* En el celular solo queda el ícono, para que la cabecera quepa en una fila. */}
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor"
             strokeWidth="1.9" strokeLinecap="round"><circle cx="12" cy="4.5" r="1.8" /><path d="M5 8.5l7 1.5 7-1.5M12 10v5l-3 6M12 15l3 6" /></svg>
        <span className="texto-boton">Accesibilidad</span>
      </button>
      {abierto && (
        <div className="panel-flotante accesibilidad" role="dialog" aria-label="Opciones de accesibilidad">
          <p className="rotulo">Tamaño del texto</p>
          <div className="segmentos" role="group" aria-label="Tamaño del texto">
            {TAMANOS.map((texto, i) => (
              <button
                key={texto}
                type="button"
                aria-pressed={preferencias.texto === i}
                className={preferencias.texto === i ? 'activo' : ''}
                onClick={() => alCambiar({ texto: i })}
              >
                {texto}
              </button>
            ))}
          </div>
          <Interruptor
            activo={preferencias.contraste}
            alCambiar={(contraste) => alCambiar({ contraste })}
            texto="Alto contraste"
            descripcion="Oscurece textos secundarios y bordes."
          />
          <Interruptor
            activo={preferencias.movimiento}
            alCambiar={(movimiento) => alCambiar({ movimiento })}
            texto="Reducir movimiento"
            descripcion="Quita animaciones y transiciones."
          />
          <p className="rotulo">Duración de los avisos</p>
          <div className="segmentos" role="group" aria-label="Duración de los avisos">
            {DURACIONES_PUSH.map((d, i) => (
              <button
                key={d.texto}
                type="button"
                aria-pressed={preferencias.duracionAvisos === i}
                className={preferencias.duracionAvisos === i ? 'activo' : ''}
                onClick={() => alCambiar({ duracionAvisos: i })}
              >
                {d.texto}
              </button>
            ))}
          </div>
          <p className="ayuda">Pasar el mouse o el foco sobre un aviso lo detiene.</p>
          <p className="ayuda nota-teclado">
            Esc cierra paneles y ventanas. Alt+N abre las notificaciones. Tab recorre la página y el
            foco se marca en amarillo.
          </p>
        </div>
      )}
    </div>
  )
}
