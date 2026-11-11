package cl.duoc.duocconecta.analitica.repository;

import cl.duoc.duocconecta.analitica.domain.RecursoExcluido;
import org.springframework.data.jpa.repository.JpaRepository;

/** Recursos que no salen en la tendencia. */
public interface RepositorioExcluidos extends JpaRepository<RecursoExcluido, String> {
}
