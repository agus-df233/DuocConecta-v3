package cl.duoc.duocconecta.analitica.service;

import cl.duoc.duocconecta.analitica.domain.EventoProcesado;
import cl.duoc.duocconecta.analitica.repository.RepositorioEventosProcesados;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotencia: anota que un consumidor procesó un registro, en la misma transacción que su efecto.
 *
 * <p>No hace falta un bloqueo: la clave lleva todos los registros de un recurso o de una persona a
 * la misma partición, y cada partición la atiende un solo hilo del grupo.</p>
 */
@Component
public class RegistroDeProcesados {

    private final RepositorioEventosProcesados repositorio;

    public RegistroDeProcesados(RepositorioEventosProcesados repositorio) {
        this.repositorio = repositorio;
    }

    /** Verdadero la primera vez; falso si ese consumidor ya lo había procesado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean primeraVez(String idEvento, String consumidor) {
        var clave = new EventoProcesado.Clave(idEvento, consumidor);
        if (repositorio.existsById(clave)) {
            return false;
        }
        repositorio.save(new EventoProcesado(clave, Instant.now()));
        return true;
    }
}
