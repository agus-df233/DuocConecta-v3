package cl.duoc.duocconecta.analitica.exception;

/**
 * El registro no tiene arreglo: reintentarlo daría el mismo resultado. Va directo a la DLT, sin
 * reintentos, para no frenar la partición.
 */
public class EventoNoProcesableException extends RuntimeException {

    public EventoNoProcesableException(String motivo) {
        super(motivo);
    }
}
