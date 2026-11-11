package cl.duoc.duocconecta.usuarios.repository;

import cl.duoc.duocconecta.usuarios.domain.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a los perfiles en el schema {@code usuarios}. */
@Repository
public interface RepositorioUsuarios extends JpaRepository<Usuario, UUID> {

    /** La que usa el auto-aprovisionamiento: el {@code oid} nunca cambia. */
    Optional<Usuario> findByOidEntra(String oidEntra);

    /** Busca el perfil por correo institucional. */
    Optional<Usuario> findByCorreo(String correo);

    /**
     * Los filtros nulos se ignoran, así el mismo query sirve para las cuatro combinaciones.
     * La comparación no distingue mayúsculas.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.visible = true AND u.ocultoPorModeracion = false
              AND (:carrera IS NULL OR LOWER(u.carrera) = LOWER(:carrera))
              AND (:sede    IS NULL OR LOWER(u.sede)    = LOWER(:sede))
            ORDER BY u.nombre ASC
            """)
    Page<Usuario> buscarVisibles(@Param("carrera") String carrera,
                                 @Param("sede") String sede,
                                 Pageable paginacion);

    /**
     * Para el perfil público: si la persona se ocultó, o la ocultó la moderación, es como si no
     * existiera.
     */
    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.visible = true AND u.ocultoPorModeracion = false")
    Optional<Usuario> findByIdAndVisibleIsTrue(@Param("id") UUID id);

    /** Los demás microservicios guardan el {@code oid}, no el id interno. Misma regla de visibilidad. */
    @Query("SELECT u FROM Usuario u WHERE u.oidEntra = :oid AND u.visible = true AND u.ocultoPorModeracion = false")
    Optional<Usuario> findByOidEntraAndVisibleIsTrue(@Param("oid") String oidEntra);

    /**
     * Para la administración: todas las personas, también las ocultas, con filtros opcionales.
     * {@code texto} busca en nombre y correo; {@code estado} es VISIBLE, OCULTA (por ella) o MODERADA.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE (:texto IS NULL OR LOWER(u.nombre) LIKE :texto OR LOWER(u.correo) LIKE :texto)
              AND (:carrera IS NULL OR LOWER(u.carrera) = LOWER(:carrera))
              AND (:rol IS NULL OR u.rol = :rol)
              AND (:estado IS NULL
                   OR (:estado = 'VISIBLE' AND u.visible = true AND u.ocultoPorModeracion = false)
                   OR (:estado = 'OCULTA' AND u.visible = false)
                   OR (:estado = 'MODERADA' AND u.ocultoPorModeracion = true))
            ORDER BY u.nombre ASC
            """)
    Page<Usuario> buscarParaAdministrar(@Param("texto") String texto, @Param("carrera") String carrera,
                                        @Param("rol") cl.duoc.duocconecta.comun.seguridad.Rol rol,
                                        @Param("estado") String estado, Pageable paginacion);

    /** Cuántas ocultó la moderación, para el dashboard. */
    long countByOcultoPorModeracionIsTrue();

    /** Total de perfiles visibles, para el dashboard de administración. */
    long countByVisibleIsTrue();

    /** Cantidad de perfiles por rol: cada fila es {@code [rol, cantidad]}. */
    @Query("SELECT u.rol, COUNT(u) FROM Usuario u GROUP BY u.rol ORDER BY COUNT(u) DESC")
    List<Object[]> contarPorRol();

    /** Cantidad de perfiles por carrera; la carrera nula llega como una fila más. */
    @Query("SELECT u.carrera, COUNT(u) FROM Usuario u GROUP BY u.carrera ORDER BY COUNT(u) DESC")
    List<Object[]> contarPorCarrera();

    /** Cantidad de perfiles por sede; la sede nula llega como una fila más. */
    @Query("SELECT u.sede, COUNT(u) FROM Usuario u GROUP BY u.sede ORDER BY COUNT(u) DESC")
    List<Object[]> contarPorSede();
}
