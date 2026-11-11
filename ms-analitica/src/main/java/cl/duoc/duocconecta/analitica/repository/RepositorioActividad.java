package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.ActividadReciente;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Actividad de las últimas 24 h. */
public interface RepositorioActividad extends JpaRepository<ActividadReciente, UUID> {

    /** Cuántas veces hizo algo una persona dentro de una ventana. */
    long countByUsuarioIdAndTipoAndOcurridoEnBetween(String usuarioId, TipoActividad tipo, Instant desde, Instant hasta);

    /** Borra lo que ya pasó el plazo de guarda. */
    @Modifying
    @Query("delete from ActividadReciente a where a.ocurridoEn < :antesDe")
    int borrarAnteriores(Instant antesDe);
}
