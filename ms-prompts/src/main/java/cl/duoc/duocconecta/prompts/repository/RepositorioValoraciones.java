package cl.duoc.duocconecta.prompts.repository;

import cl.duoc.duocconecta.prompts.domain.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Valoraciones de prompts. */
public interface RepositorioValoraciones extends JpaRepository<Valoracion, Valoracion.Clave> {

    /** Total de valoraciones en la biblioteca, para el dashboard. */
    @Query("select count(v) from Valoracion v")
    long total();
}
