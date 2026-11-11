package cl.duoc.duocconecta.mensajeria.controller;

import cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso;
import cl.duoc.duocconecta.mensajeria.dto.kafka.AumentarParticionesDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ClusterKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.CrearTopicoKafkaDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.GrupoKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ModificarConfiguracionDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ResetearOffsetsDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.TopicoKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.service.ServicioAdministracionKafka;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de Kafka: el clúster, los tópicos y su configuración, y los grupos de
 * consumidores con sus offsets y su lag. Solo ADMINISTRADOR, y solo tópicos {@code duocconecta.}.
 */
@RestController
@RequestMapping("/api/v1/mensajeria/kafka")
@PreAuthorize(ReglasDeAcceso.SOLO_ADMINISTRADOR)
@ConditionalOnProperty(prefix = "duocconecta.kafka", name = "habilitada", havingValue = "true")
@Tag(name = "Kafka", description = "Tópicos, configuración, grupos de consumidores, offsets y lag")
@SecurityRequirement(name = "bearer-jwt")
public class ControladorKafka {

    private final ServicioAdministracionKafka servicio;

    public ControladorKafka(ServicioAdministracionKafka servicio) {
        this.servicio = servicio;
    }

    /** Los brokers vivos y el controlador. */
    @Operation(summary = "Estado del clúster", description = "Brokers vivos y cuál es el controlador.")
    @GetMapping("/cluster")
    public ClusterKafkaRespuesta cluster() {
        return servicio.cluster();
    }

    /** Los tópicos de la plataforma. */
    @Operation(summary = "Lista los tópicos", description = "Solo los de prefijo duocconecta., con su configuración y registros guardados.")
    @GetMapping("/topicos")
    public List<TopicoKafkaRespuesta> listarTopicos() {
        return servicio.listarTopicos();
    }

    /** Un tópico: por partición, el líder, las réplicas, el ISR y los offsets. */
    @Operation(summary = "Detalle de un tópico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle"),
            @ApiResponse(responseCode = "404", description = "El tópico no existe", content = @Content)
    })
    @GetMapping("/topicos/{nombre}")
    public TopicoKafkaRespuesta detalleTopico(@PathVariable String nombre) {
        return servicio.detalleTopico(nombre);
    }

    /** Crea un tópico. */
    @Operation(summary = "Crea un tópico", description = "Con sus particiones, factor de replicación y configuración (retención, limpieza…).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tópico creado"),
            @ApiResponse(responseCode = "400", description = "Nombre, RF o configuración inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Ya existe", content = @Content)
    })
    @PostMapping("/topicos")
    public ResponseEntity<TopicoKafkaRespuesta> crearTopico(@Valid @RequestBody CrearTopicoKafkaDatos datos) {
        TopicoKafkaRespuesta creado = servicio.crearTopico(datos);
        return ResponseEntity.created(URI.create("/api/v1/mensajeria/kafka/topicos/" + creado.nombre())).body(creado);
    }

    /** Cambia la retención, la limpieza u otra configuración, sin recrear el tópico. */
    @Operation(summary = "Modifica la configuración de un tópico",
            description = "Por ejemplo, la retención. También en los tópicos base: cambia cuánto se guarda, no el orden.")
    @PatchMapping("/topicos/{nombre}/configuracion")
    public TopicoKafkaRespuesta modificarConfiguracion(@PathVariable String nombre,
                                                       @Valid @RequestBody ModificarConfiguracionDatos datos) {
        return servicio.modificarConfiguracion(nombre, datos);
    }

    /** Aumenta las particiones. */
    @Operation(summary = "Aumenta las particiones de un tópico",
            description = "Solo aumenta. En los tópicos base responde 409: rompería el orden por clave.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Particiones aumentadas"),
            @ApiResponse(responseCode = "400", description = "El total no es mayor al actual", content = @Content),
            @ApiResponse(responseCode = "409", description = "Tópico de la topología base", content = @Content)
    })
    @PostMapping("/topicos/{nombre}/particiones")
    public TopicoKafkaRespuesta aumentarParticiones(@PathVariable String nombre,
                                                    @Valid @RequestBody AumentarParticionesDatos datos) {
        return servicio.aumentarParticiones(nombre, datos);
    }

    /** Borra un tópico. */
    @Operation(summary = "Elimina un tópico", description = "Los cuatro de la topología base están protegidos (409).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminado"),
            @ApiResponse(responseCode = "409", description = "Tópico de la topología base", content = @Content)
    })
    @DeleteMapping("/topicos/{nombre}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String nombre) {
        servicio.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }

    /** Los grupos de consumidores con su estado y su lag total. */
    @Operation(summary = "Lista los grupos de consumidores")
    @GetMapping("/grupos")
    public List<GrupoKafkaRespuesta> listarGrupos() {
        return servicio.listarGrupos();
    }

    /** Un grupo: miembros, particiones asignadas, offsets confirmados y lag por partición. */
    @Operation(summary = "Detalle de un grupo", description = "Quién atiende cada partición, dónde va y cuánto le falta (lag).")
    @GetMapping("/grupos/{id}")
    public GrupoKafkaRespuesta detalleGrupo(@PathVariable String id) {
        return servicio.detalleGrupo(id);
    }

    /** Mueve los offsets de un grupo vacío: para releer o saltarse lo pendiente. */
    @Operation(summary = "Mueve los offsets de un grupo",
            description = "Al inicio, al final o a una fecha. Solo con el grupo vacío (409 si tiene consumidores conectados).")
    @PostMapping("/grupos/{id}/offsets")
    public GrupoKafkaRespuesta resetearOffsets(@PathVariable String id, @Valid @RequestBody ResetearOffsetsDatos datos) {
        return servicio.resetearOffsets(id, datos);
    }
}
