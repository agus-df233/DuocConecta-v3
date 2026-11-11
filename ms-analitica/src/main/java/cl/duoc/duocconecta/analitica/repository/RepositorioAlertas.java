package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.Alerta;
import cl.duoc.duocconecta.comun.eventos.kafka.TipoActividad;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Alertas de actividad. */
public interface RepositorioAlertas extends JpaRepository<Alerta, UUID> {

    /** La alerta de la misma ráfaga, si existe: una que terminó dentro de la ventana. */
    Optional<Alerta> findFirstByUsuarioIdAndTipoAndHastaGreaterThanEqualOrderByHastaDesc(
            String usuarioId, TipoActividad tipo, Instant desde);

    /** Las más recientes primero. */
    List<Alerta> findByRevisadaOrderByCreadaEnDesc(boolean revisada, Pageable pagina);

    /** Todas, las más recientes primero. */
    List<Alerta> findAllByOrderByCreadaEnDesc(Pageable pagina);

    long countByRevisadaFalse();
}
