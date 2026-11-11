package cl.duoc.duocconecta.prompts.exception;

public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
