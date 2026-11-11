package cl.duoc.duocconecta.usuarios.service;

/** El perfil ya está en el estado pedido: ocultar uno oculto, o mostrar uno que no lo está. */
public class ConflictoDeModeracionException extends RuntimeException {

    public ConflictoDeModeracionException(String mensaje) {
        super(mensaje);
    }
}
