package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.MetricaHora;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Registros por hora. */
public interface RepositorioMetricas extends JpaRepository<MetricaHora, MetricaHora.Clave> {

    /** Las horas desde un instante, en orden. */
    @Query("select m from MetricaHora m where m.clave.hora >= :desde order by m.clave.hora")
    List<MetricaHora> desde(Instant desde);

    /** Borra las horas viejas. */
    @Modifying
    @Query("delete from MetricaHora m where m.clave.hora < :antesDe")
    int borrarAnteriores(Instant antesDe);
}
