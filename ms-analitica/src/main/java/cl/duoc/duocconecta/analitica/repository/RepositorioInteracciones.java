package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.InteraccionDiaria;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoRecurso;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Conteos diarios de interacciones. */
public interface RepositorioInteracciones extends JpaRepository<InteraccionDiaria, InteraccionDiaria.Clave> {

    /** Los conteos de un tipo de recurso desde un día, sin los recursos excluidos. */
    @Query("""
            select i from InteraccionDiaria i
            where i.clave.tipoRecurso = :tipoRecurso and i.clave.dia >= :desde
              and i.clave.recursoId not in (select e.recursoId from RecursoExcluido e)""")
    List<InteraccionDiaria> vigentes(TipoRecurso tipoRecurso, LocalDate desde);

    /** Los conteos de un recurso desde un día. */
    @Query("select i from InteraccionDiaria i where i.clave.recursoId = :recursoId and i.clave.dia >= :desde")
    List<InteraccionDiaria> deRecurso(String recursoId, LocalDate desde);

    /** Borra los conteos de días que ya no entran en ninguna ventana. */
    @Modifying
    @Query("delete from InteraccionDiaria i where i.clave.dia < :antesDe")
    int borrarAnteriores(LocalDate antesDe);
}
