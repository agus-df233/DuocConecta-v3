package cl.duoc.duocconecta.analitica.consumidor;

import cl.duoc.duocconecta.analitica.exception.EventoNoProcesableException;
import java.util.Objects;

/** Validaciones que no caben en las anotaciones del DTO porque cruzan el cuerpo con la clave. */
final class ReglasDeRegistro {

    private ReglasDeRegistro() {
    }

    /**
     * La clave del registro tiene que ser la del cuerpo. Si no, el registro cayó en otra partición
     * que el resto de su recurso o de su persona, y procesarlo rompería el orden.
     */
    static void exigirClave(String clave, String esperada, String campo) {
        if (!Objects.equals(clave, esperada)) {
            throw new EventoNoProcesableException(
                    "La clave del registro (" + clave + ") no coincide con " + campo + " (" + esperada + ")");
        }
    }
}
