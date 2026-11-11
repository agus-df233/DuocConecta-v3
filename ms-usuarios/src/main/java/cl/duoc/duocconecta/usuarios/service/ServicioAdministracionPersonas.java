package cl.duoc.duocconecta.usuarios.service;

import cl.duoc.duocconecta.comun.seguridad.Rol;
import cl.duoc.duocconecta.usuarios.domain.Usuario;
import cl.duoc.duocconecta.usuarios.dto.PersonaAdminRespuesta;
import cl.duoc.duocconecta.usuarios.repository.RepositorioUsuarios;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de personas para el ADMINISTRADOR: buscar en toda la comunidad (también las ocultas) y
 * sacar un perfil de los listados públicos por moderación, o devolverlo.
 *
 * <p>Ocultar por moderación no borra nada ni bloquea la cuenta: la persona sigue entrando y viendo
 * lo suyo, pero no aparece en los listados ni como autora. Cada acción queda en el log con el
 * {@code oid} del administrador.</p>
 */
@Service
public class ServicioAdministracionPersonas {

    private static final Logger log = LoggerFactory.getLogger(ServicioAdministracionPersonas.class);
    private static final Set<String> ESTADOS = Set.of("VISIBLE", "OCULTA", "MODERADA");

    private final RepositorioUsuarios repositorio;

    public ServicioAdministracionPersonas(RepositorioUsuarios repositorio) {
        this.repositorio = repositorio;
    }

    /** Busca en toda la comunidad. Los filtros vacíos se ignoran. */
    @Transactional(readOnly = true)
    public Page<PersonaAdminRespuesta> buscar(String texto, String carrera, Rol rol, String estado, Pageable pagina) {
        String patron = vacio(texto) ? null : "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
        String estadoNormal = vacio(estado) ? null : estado.trim().toUpperCase(Locale.ROOT);
        if (estadoNormal != null && !ESTADOS.contains(estadoNormal)) {
            estadoNormal = null;
        }
        return repositorio.buscarParaAdministrar(patron, vacio(carrera) ? null : carrera.trim(), rol, estadoNormal, pagina)
                .map(PersonaAdminRespuesta::desde);
    }

    /** Una persona por su {@code oid}, también si está oculta. */
    @Transactional(readOnly = true)
    public PersonaAdminRespuesta porOid(String oid) {
        return repositorio.findByOidEntra(oid).map(PersonaAdminRespuesta::desde)
                .orElseThrow(() -> new UsuarioNoEncontradoException(oid));
    }

    /** La saca de los listados públicos. 409 si ya lo estaba. */
    @Transactional
    public PersonaAdminRespuesta ocultar(UUID id, String motivo, String administrador) {
        Usuario usuario = buscar(id);
        if (usuario.isOcultoPorModeracion()) {
            throw new ConflictoDeModeracionException("El perfil ya está oculto por moderación");
        }
        usuario.ocultarPorModeracion(motivo.trim(), administrador);
        log.warn("Moderación: {} ocultó el perfil {} ({})", administrador, usuario.getOidEntra(), motivo.trim());
        return PersonaAdminRespuesta.desde(usuario);
    }

    /** La devuelve a los listados. 409 si no estaba oculta por moderación. */
    @Transactional
    public PersonaAdminRespuesta mostrar(UUID id, String administrador) {
        Usuario usuario = buscar(id);
        if (!usuario.isOcultoPorModeracion()) {
            throw new ConflictoDeModeracionException("El perfil no está oculto por moderación");
        }
        usuario.mostrarPorModeracion();
        log.info("Moderación: {} devolvió el perfil {} a los listados", administrador, usuario.getOidEntra());
        return PersonaAdminRespuesta.desde(usuario);
    }

    private Usuario buscar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
