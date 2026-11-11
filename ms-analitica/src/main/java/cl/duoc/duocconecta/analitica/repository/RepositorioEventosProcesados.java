package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.EventoProcesado;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Registro de idempotencia de los consumidores. */
public interface RepositorioEventosProcesados extends JpaRepository<EventoProcesado, EventoProcesado.Clave> {

    /** Pasada la retención del tópico, un registro ya no puede volver a llegar. */
    @Modifying
    @Query("delete from EventoProcesado e where e.procesadoEn < :antesDe")
    int borrarAnteriores(Instant antesDe);
}
