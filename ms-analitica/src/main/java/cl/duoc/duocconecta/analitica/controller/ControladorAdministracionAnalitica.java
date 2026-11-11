package cl.duoc.duocconecta.analitica.controller;

import static cl.duoc.duocconecta.comun.seguridad.ReglasDeAcceso.SOLO_ADMINISTRADOR;

import cl.duoc.duocconecta.analitica.dto.AlertaRespuesta;
import cl.duoc.duocconecta.analitica.dto.EstadoConsumidor;
import cl.duoc.duocconecta.analitica.dto.MetricaHoraRespuesta;
import cl.duoc.duocconecta.analitica.service.ServicioAlertas;
import cl.duoc.duocconecta.analitica.service.ServicioMetricas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lo que ve y hace el administrador: alertas, métricas y la pausa de los consumidores de Kafka. */
@RestController
@RequestMapping("/api/v1/analitica/admin")
@PreAuthorize(SOLO_ADMINISTRADOR)
@Validated
@Tag(name = "Administración de la analítica", description = "Solo ADMINISTRADOR")
public class ControladorAdministracionAnalitica {

    private static final Logger log = LoggerFactory.getLogger(ControladorAdministracionAnalitica.class);

    private final ServicioAlertas alertas;
    private final ServicioMetricas metricas;
    private final KafkaListenerEndpointRegistry registro;

    public ControladorAdministracionAnalitica(ServicioAlertas alertas, ServicioMetricas metricas,
                                              KafkaListenerEndpointRegistry registro) {
        this.alertas = alertas;
        this.metricas = metricas;
        this.registro = registro;
    }

    /** Las alertas, las más recientes primero. */
    @Operation(summary = "Lista las alertas de actividad")
    @GetMapping("/alertas")
    public List<AlertaRespuesta> alertas(
            @Parameter(description = "Solo las que falta revisar") @RequestParam(defaultValue = "false") boolean pendientes,
            @Parameter(description = "Solo las de esta persona (oid)") @RequestParam(required = false) String usuarioId) {
        return alertas.listar(pendientes, usuarioId);
    }

    /** Cuántas faltan por revisar, para el contador del menú. */
    @Operation(summary = "Cuenta las alertas pendientes")
    @GetMapping("/alertas/pendientes")
    public Map<String, Long> pendientes() {
        return Map.of("cantidad", alertas.pendientes());
    }

    /** Marca una alerta como revisada. */
    @Operation(summary = "Marca una alerta como revisada")
    @PatchMapping("/alertas/{id}/revisada")
    public AlertaRespuesta revisada(@PathVariable UUID id) {
        return alertas.marcarRevisada(id);
    }

    /** Registros por hora de los dos tópicos. */
    @Operation(summary = "Registros por hora, por tópico y por tipo")
    @GetMapping("/metricas")
    public List<MetricaHoraRespuesta> metricas(
            @Parameter(description = "Cuántas horas hacia atrás, de 1 a 168") @RequestParam(defaultValue = "24")
            @Min(1) @Max(168) int horas) {
        return metricas.serie(horas);
    }

    /** Estado de cada consumidor y qué particiones atiende. */
    @Operation(summary = "Estado de los consumidores de Kafka")
    @GetMapping("/consumidores")
    public List<EstadoConsumidor> consumidores() {
        return registro.getListenerContainers().stream()
                .sorted(Comparator.comparing(MessageListenerContainer::getListenerId))
                .map(ControladorAdministracionAnalitica::estado)
                .toList();
    }

    /**
     * Pausa los consumidores. Siguen en su grupo (no hay rebalanceo), pero dejan de leer: los
     * registros nuevos se acumulan y el lag sube en Kafka UI. Es la demo del desacoplamiento.
     */
    @Operation(summary = "Pausa los consumidores de Kafka",
            description = "Siguen en el grupo pero no leen: el lag sube. Al reanudar se ponen al día solos.")
    @PostMapping("/consumidores/pausa")
    public List<EstadoConsumidor> pausar() {
        registro.getListenerContainers().forEach(MessageListenerContainer::pause);
        log.warn("Consumidores de Kafka en pausa: el lag va a subir hasta que se reanuden");
        return consumidores();
    }

    /** Los reanuda: leen desde su último offset confirmado, sin perder nada. */
    @Operation(summary = "Reanuda los consumidores de Kafka")
    @PostMapping("/consumidores/reanudar")
    public List<EstadoConsumidor> reanudar() {
        registro.getListenerContainers().forEach(MessageListenerContainer::resume);
        log.info("Consumidores de Kafka reanudados: siguen desde su último offset");
        return consumidores();
    }

    private static EstadoConsumidor estado(MessageListenerContainer contenedor) {
        var propiedades = contenedor.getContainerProperties();
        List<String> topicos = propiedades.getTopics() == null ? List.of() : Arrays.asList(propiedades.getTopics());
        var asignadas = contenedor.getAssignedPartitions();
        List<String> particiones = asignadas == null ? List.of()
                : asignadas.stream().map(p -> p.topic() + "-" + p.partition()).sorted().toList();
        return new EstadoConsumidor(contenedor.getListenerId(), contenedor.getGroupId(), topicos,
                contenedor.isRunning(), contenedor.isPauseRequested(), particiones);
    }
}
