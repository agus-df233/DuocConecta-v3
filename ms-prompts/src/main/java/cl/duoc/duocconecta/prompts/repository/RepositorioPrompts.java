package cl.duoc.duocconecta.prompts.repository;

import cl.duoc.duocconecta.prompts.domain.CategoriaPrompt;
import cl.duoc.duocconecta.prompts.domain.Prompt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Prompts de la biblioteca. */
public interface RepositorioPrompts extends JpaRepository<Prompt, UUID> {

    /**
     * Lo que una persona puede ver, con los filtros que vengan (cada uno es opcional): los
     * públicos no ocultos de todos, más los suyos en cualquier estado.
     */
    @Query("""
            select p from Prompt p
            where (p.autorId = :usuarioId or (p.visibilidad = cl.duoc.duocconecta.prompts.domain.Visibilidad.PUBLICO
                                              and p.oculto = false))
              and (:categoria is null or p.categoria = :categoria)
              and (:carrera is null or p.carrera = :carrera)
              and (:herramienta is null or lower(p.herramienta) = lower(:herramienta))
              and (:texto is null or lower(p.titulo) like :texto or lower(p.proposito) like :texto)
            order by p.creadoEn desc""")
    List<Prompt> buscarVisibles(String usuarioId, CategoriaPrompt categoria, String carrera, String herramienta,
                                String texto, Pageable pagina);

    /** Los de una persona, los más nuevos primero. */
    List<Prompt> findByAutorIdOrderByCreadoEnDesc(String autorId);

    /** Para moderar: todos, con filtro opcional por ocultos. */
    @Query("""
            select p from Prompt p
            where (:oculto is null or p.oculto = :oculto) and (:autorId is null or p.autorId = :autorId)
            order by p.creadoEn desc""")
    List<Prompt> paraModerar(Boolean oculto, String autorId, Pageable pagina);

    long countByOcultoTrue();

    long countByVisibilidadAndOcultoFalse(cl.duoc.duocconecta.prompts.domain.Visibilidad visibilidad);

    /** Cuántos hay por categoría. */
    @Query("select p.categoria, count(p) from Prompt p group by p.categoria order by count(p) desc")
    List<Object[]> contarPorCategoria();

    /** Cuántos hay por carrera. */
    @Query("select coalesce(p.carrera, 'Sin carrera'), count(p) from Prompt p group by p.carrera order by count(p) desc")
    List<Object[]> contarPorCarrera();

    /** Borrar el original deja a sus copias sin enlace (en la base lo hace el ON DELETE SET NULL). */
    @Modifying
    @Query("update Prompt p set p.origen = null where p.origen.id = :origenId")
    int soltarCopias(UUID origenId);
}
