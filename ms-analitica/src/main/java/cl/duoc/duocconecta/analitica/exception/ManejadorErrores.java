package cl.duoc.duocconecta.analitica.exception;

import cl.duoc.duocconecta.comun.seguridad.ManejadorErroresBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores propios de ms-analitica. Los comunes vienen de {@link ManejadorErroresBase}. */
@RestControllerAdvice
public class ManejadorErrores extends ManejadorErroresBase {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException excepcion) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", excepcion.getMessage());
    }
}
