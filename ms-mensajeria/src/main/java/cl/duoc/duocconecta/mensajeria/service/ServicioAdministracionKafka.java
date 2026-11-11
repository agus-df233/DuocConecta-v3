package cl.duoc.duocconecta.mensajeria.service;

import cl.duoc.duocconecta.comun.eventos.kafka.PropiedadesKafka;
import cl.duoc.duocconecta.mensajeria.dto.kafka.AumentarParticionesDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.BrokerRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ClusterKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.CrearTopicoKafkaDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.GrupoKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.LagParticionRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.MiembroGrupoRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ModificarConfiguracionDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ParticionRespuesta;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ReglasKafka;
import cl.duoc.duocconecta.mensajeria.dto.kafka.ResetearOffsetsDatos;
import cl.duoc.duocconecta.mensajeria.dto.kafka.TopicoKafkaRespuesta;
import cl.duoc.duocconecta.mensajeria.exception.ConflictoDeEstadoException;
import cl.duoc.duocconecta.mensajeria.exception.KafkaNoDisponibleException;
import cl.duoc.duocconecta.mensajeria.exception.RecursoNoEncontradoException;
import cl.duoc.duocconecta.mensajeria.exception.SolicitudInvalidaException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Administración de Kafka con el {@code AdminClient}: tópicos, sus configuraciones y los grupos
 * de consumidores. Es lo mismo que hace {@code docker/kafka/kafka.sh} desde la línea de comandos.
 *
 * <p>Solo toca tópicos con el prefijo {@code duocconecta.} (400 si no). Los cuatro de la topología
 * base no se borran ni se les cambian las particiones (409): más particiones cambian a qué
 * partición va cada clave, y los registros nuevos de un recurso quedarían separados de los viejos.
 * Su retención y su limpieza sí se pueden ajustar.</p>
 */
@Service
@ConditionalOnProperty(prefix = "duocconecta.kafka", name = "habilitada", havingValue = "true")
public class ServicioAdministracionKafka {

    private static final Logger log = LoggerFactory.getLogger(ServicioAdministracionKafka.class);
    private static final long ESPERA_SEGUNDOS = 10;

    /** Lo que se muestra de la configuración aunque tenga el valor por defecto del broker. */
    private static final Set<String> CONFIGURACION_VISIBLE = Set.of(
            "retention.ms", "cleanup.policy", "segment.ms", "min.insync.replicas");

    private final Admin admin;
    private final Set<String> protegidos;

    public ServicioAdministracionKafka(Admin admin, PropiedadesKafka propiedades) {
        this.admin = admin;
        Set<String> base = new LinkedHashSet<>();
        propiedades.getTopicos().keySet().forEach(clave -> {
            base.add(propiedades.topico(clave));
            base.add(propiedades.dlt(clave));
        });
        this.protegidos = Set.copyOf(base);
    }

    /** Los brokers vivos y cuál es el controlador. */
    public ClusterKafkaRespuesta cluster() {
        var descripcion = admin.describeCluster();
        Collection<Node> nodos = esperar(descripcion.nodes());
        Node controlador = esperar(descripcion.controller());
        List<BrokerRespuesta> brokers = nodos.stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerRespuesta(n.id(), n.host(), n.port(), controlador != null && n.id() == controlador.id()))
                .toList();
        return new ClusterKafkaRespuesta(esperar(descripcion.clusterId()),
                controlador == null ? -1 : controlador.id(), brokers);
    }

    /** Los tópicos de la plataforma, sin el detalle por partición. */
    public List<TopicoKafkaRespuesta> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics().names()).stream()
                .filter(n -> n.startsWith(ReglasKafka.PREFIJO))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        Map<String, Map<String, String>> configuraciones = configuraciones(nombres);
        Map<TopicPartition, long[]> offsets = offsets(descripciones.values());
        return nombres.stream().map(n -> respuesta(descripciones.get(n), configuraciones.get(n), offsets, false)).toList();
    }

    /** Un tópico con sus particiones: líder, réplicas, ISR y offsets. */
    public TopicoKafkaRespuesta detalleTopico(String nombre) {
        exigirDeLaPlataforma(nombre);
        TopicDescription descripcion = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        return respuesta(descripcion, configuraciones(Set.of(nombre)).get(nombre), offsets(List.of(descripcion)), true);
    }

    /** Crea un tópico con sus particiones, réplicas y configuración. */
    public TopicoKafkaRespuesta crearTopico(CrearTopicoKafkaDatos datos) {
        exigirDeLaPlataforma(datos.nombre());
        int brokers = esperar(admin.describeCluster().nodes()).size();
        if (datos.replicacion() > brokers) {
            throw new SolicitudInvalidaException("El factor de replicación (" + datos.replicacion()
                    + ") no puede superar la cantidad de brokers vivos (" + brokers + ")");
        }
        Map<String, String> configuracion = validarConfiguracion(datos.configuracion());
        var nuevo = new NewTopic(datos.nombre(), datos.particiones(), datos.replicacion()).configs(configuracion);
        esperar(admin.createTopics(List.of(nuevo)).all());
        log.info("Tópico {} creado: {} particiones, RF {}, {}", datos.nombre(), datos.particiones(),
                datos.replicacion(), configuracion);
        return detalleCuando(datos.nombre(), t -> t.particiones().size() == datos.particiones());
    }

    /** Cambia la configuración de un tópico (retención, limpieza…) sin recrearlo. */
    public TopicoKafkaRespuesta modificarConfiguracion(String nombre, ModificarConfiguracionDatos datos) {
        exigirDeLaPlataforma(nombre);
        if (datos.claves().isEmpty()) {
            throw new SolicitudInvalidaException("No hay nada que cambiar: usa 'fijar' o 'quitar'");
        }
        Map<String, String> fijar = validarConfiguracion(datos.fijar());
        List<String> quitar = datos.quitar() == null ? List.of() : datos.quitar();
        quitar.forEach(ServicioAdministracionKafka::exigirConfiguracionPermitida);

        List<AlterConfigOp> operaciones = new ArrayList<>();
        fijar.forEach((clave, valor) -> operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        quitar.forEach(clave -> operaciones.add(new AlterConfigOp(new ConfigEntry(clave, ""), AlterConfigOp.OpType.DELETE)));
        var recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        esperar(admin.incrementalAlterConfigs(Map.of(recurso, operaciones)).all());
        log.info("Tópico {}: configuración cambiada (fijar {}, quitar {})", nombre, fijar, quitar);
        // Los brokers aplican el cambio en segundos: se responde cuando ya se ve.
        return detalleCuando(nombre, t -> fijar.entrySet().stream()
                .allMatch(e -> e.getValue().equals(t.configuracion().get(e.getKey()))));
    }

    /** Aumenta las particiones. Kafka no permite bajarlas. */
    public TopicoKafkaRespuesta aumentarParticiones(String nombre, AumentarParticionesDatos datos) {
        exigirDeLaPlataforma(nombre);
        if (protegidos.contains(nombre)) {
            throw new ConflictoDeEstadoException("No se cambian las particiones de " + nombre
                    + ": cambiaría a qué partición va cada clave y se rompería el orden por recurso o por persona");
        }
        int actuales = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre).partitions().size();
        if (datos.total() <= actuales) {
            throw new SolicitudInvalidaException("El tópico ya tiene " + actuales
                    + " particiones: Kafka solo permite aumentarlas");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(datos.total()))).all());
        log.info("Tópico {}: de {} a {} particiones", nombre, actuales, datos.total());
        return detalleCuando(nombre, t -> t.particiones().size() == datos.total());
    }

    /** Borra un tópico. Los de la topología base están protegidos. */
    public void eliminarTopico(String nombre) {
        exigirDeLaPlataforma(nombre);
        if (protegidos.contains(nombre)) {
            throw new ConflictoDeEstadoException("El tópico " + nombre + " es de la topología base y no se borra");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Tópico {} eliminado", nombre);
    }

    /** Los grupos de consumidores, con su estado y su lag total. */
    public List<GrupoKafkaRespuesta> listarGrupos() {
        List<String> ids = esperar(admin.listGroups(ListGroupsOptions.forConsumerGroups()).all()).stream()
                .map(g -> g.groupId()).sorted().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, ConsumerGroupDescription> descripciones = esperar(admin.describeConsumerGroups(ids).all());
        return ids.stream().map(id -> {
            List<LagParticionRespuesta> lag = lag(id, descripciones.get(id));
            return new GrupoKafkaRespuesta(id, estado(descripciones.get(id)), descripciones.get(id).partitionAssignor(),
                    lag.stream().mapToLong(LagParticionRespuesta::lag).sum(), List.of(), List.of());
        }).toList();
    }

    /** Un grupo: quién está conectado, qué particiones atiende cada uno, offsets y lag. */
    public GrupoKafkaRespuesta detalleGrupo(String id) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(id)).all()).get(id);
        if (descripcion == null || "DEAD".equals(estado(descripcion))) {
            throw new RecursoNoEncontradoException("El grupo " + id + " no existe");
        }
        List<MiembroGrupoRespuesta> miembros = descripcion.members().stream()
                .map(m -> new MiembroGrupoRespuesta(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .map(p -> p.topic() + "-" + p.partition()).sorted().toList()))
                .sorted(Comparator.comparing(MiembroGrupoRespuesta::cliente))
                .toList();
        List<LagParticionRespuesta> lag = lag(id, descripcion);
        return new GrupoKafkaRespuesta(id, estado(descripcion), descripcion.partitionAssignor(),
                lag.stream().mapToLong(LagParticionRespuesta::lag).sum(), miembros, lag);
    }

    /**
     * Mueve los offsets de un grupo: al inicio para releer todo lo que la retención conserva (y
     * reconstruir una vista), al final para saltarse lo pendiente, o a una fecha. Solo con el grupo
     * vacío: si hay consumidores conectados, ellos confirmarían encima.
     */
    public GrupoKafkaRespuesta resetearOffsets(String id, ResetearOffsetsDatos datos) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(id)).all()).get(id);
        if (!descripcion.members().isEmpty()) {
            throw new ConflictoDeEstadoException("El grupo " + id + " tiene " + descripcion.members().size()
                    + " consumidores conectados: detén el servicio antes de mover sus offsets (pausar no basta)");
        }
        if (datos.destino() == ResetearOffsetsDatos.Destino.FECHA && datos.fecha() == null) {
            throw new SolicitudInvalidaException("Con destino FECHA hay que indicar la fecha");
        }
        Set<TopicPartition> particiones = esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata()).keySet()
                .stream()
                .filter(p -> datos.topico() == null || datos.topico().isBlank() || p.topic().equals(datos.topico()))
                .collect(Collectors.toSet());
        if (particiones.isEmpty()) {
            throw new SolicitudInvalidaException("El grupo " + id + " no tiene offsets confirmados"
                    + (datos.topico() == null ? "" : " en " + datos.topico()));
        }
        OffsetSpec especificacion = switch (datos.destino()) {
            case INICIO -> OffsetSpec.earliest();
            case FIN -> OffsetSpec.latest();
            case FECHA -> OffsetSpec.forTimestamp(datos.fecha().toEpochMilli());
        };
        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        particiones.forEach(p -> pedido.put(p, especificacion));
        var destinos = esperar(admin.listOffsets(pedido).all());
        Map<TopicPartition, OffsetAndMetadata> nuevos = new HashMap<>();
        destinos.forEach((p, info) -> nuevos.put(p, new OffsetAndMetadata(Math.max(info.offset(), 0))));
        esperar(admin.alterConsumerGroupOffsets(id, nuevos).all());
        log.warn("Grupo {}: offsets movidos a {} en {} particiones", id, datos.destino(), nuevos.size());
        return detalleGrupo(id);
    }

    // --- Ayudantes ---

    /**
     * El detalle, cuando el cambio ya se ve. Un tópico recién creado tarda unos milisegundos en
     * aparecer en la metadata de todos los brokers: preguntar al tiro daría 404.
     */
    private TopicoKafkaRespuesta detalleCuando(String nombre, java.util.function.Predicate<TopicoKafkaRespuesta> listo) {
        RuntimeException ultimo = null;
        for (int intento = 0; intento < 20; intento++) {
            try {
                TopicoKafkaRespuesta detalle = detalleTopico(nombre);
                if (listo.test(detalle)) {
                    return detalle;
                }
            } catch (RecursoNoEncontradoException | KafkaNoDisponibleException error) {
                ultimo = error;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (ultimo != null) {
            throw ultimo;
        }
        return detalleTopico(nombre);
    }

    private List<LagParticionRespuesta> lag(String id, ConsumerGroupDescription descripcion) {
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());
        Map<TopicPartition, String> dueno = new HashMap<>();
        descripcion.members().forEach(m -> m.assignment().topicPartitions().forEach(p -> dueno.put(p, m.clientId())));
        Set<TopicPartition> todas = new LinkedHashSet<>(confirmados.keySet());
        todas.addAll(dueno.keySet());
        if (todas.isEmpty()) {
            return List.of();
        }
        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        todas.forEach(p -> pedido.put(p, OffsetSpec.latest()));
        var finales = esperar(admin.listOffsets(pedido).all());
        return todas.stream()
                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                .map(p -> {
                    long fin = finales.get(p).offset();
                    OffsetAndMetadata confirmado = confirmados.get(p);
                    long hasta = confirmado == null ? 0 : confirmado.offset();
                    return new LagParticionRespuesta(p.topic(), p.partition(), hasta, fin, Math.max(fin - hasta, 0),
                            dueno.getOrDefault(p, ""));
                })
                .toList();
    }

    private TopicoKafkaRespuesta respuesta(TopicDescription descripcion, Map<String, String> configuracion,
                                           Map<TopicPartition, long[]> offsets, boolean conParticiones) {
        List<ParticionRespuesta> particiones = descripcion.partitions().stream().map(p -> {
            long[] rango = offsets.getOrDefault(new TopicPartition(descripcion.name(), p.partition()), new long[]{0, 0});
            return new ParticionRespuesta(p.partition(), p.leader() == null ? -1 : p.leader().id(),
                    p.replicas().stream().map(Node::id).toList(), p.isr().stream().map(Node::id).toList(),
                    rango[0], rango[1]);
        }).toList();
        long registros = particiones.stream().mapToLong(p -> p.offsetFinal() - p.offsetInicial()).sum();
        int replicacion = descripcion.partitions().isEmpty() ? 0 : descripcion.partitions().get(0).replicas().size();
        return new TopicoKafkaRespuesta(descripcion.name(), protegidos.contains(descripcion.name()), replicacion,
                conParticiones ? particiones : List.of(), registros, configuracion);
    }

    /** Offset inicial y final de cada partición. */
    private Map<TopicPartition, long[]> offsets(Collection<TopicDescription> descripciones) {
        Map<TopicPartition, OffsetSpec> inicio = new HashMap<>();
        Map<TopicPartition, OffsetSpec> fin = new HashMap<>();
        descripciones.forEach(d -> d.partitions().forEach(p -> {
            var tp = new TopicPartition(d.name(), p.partition());
            inicio.put(tp, OffsetSpec.earliest());
            fin.put(tp, OffsetSpec.latest());
        }));
        var iniciales = esperar(admin.listOffsets(inicio).all());
        var finales = esperar(admin.listOffsets(fin).all());
        Map<TopicPartition, long[]> rangos = new HashMap<>();
        iniciales.forEach((tp, info) -> rangos.put(tp, new long[]{info.offset(), finales.get(tp).offset()}));
        return rangos;
    }

    /** Lo fijado en cada tópico, más la retención y la limpieza aunque sean las del broker. */
    private Map<String, Map<String, String>> configuraciones(Set<String> nombres) {
        List<ConfigResource> recursos = nombres.stream()
                .map(n -> new ConfigResource(ConfigResource.Type.TOPIC, n)).toList();
        Map<ConfigResource, Config> configs = esperar(admin.describeConfigs(recursos).all());
        Map<String, Map<String, String>> resultado = new HashMap<>();
        configs.forEach((recurso, config) -> resultado.put(recurso.name(), config.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG
                        || CONFIGURACION_VISIBLE.contains(e.name()))
                .collect(Collectors.toMap(ConfigEntry::name, e -> e.value() == null ? "" : e.value(),
                        (a, b) -> a, TreeMap::new))));
        return resultado;
    }

    private static Map<String, String> validarConfiguracion(Map<String, String> configuracion) {
        if (configuracion == null) {
            return Map.of();
        }
        Map<String, String> validada = new TreeMap<>();
        configuracion.forEach((clave, valor) -> {
            exigirConfiguracionPermitida(clave);
            if (valor == null || valor.isBlank()) {
                throw new SolicitudInvalidaException("La configuración " + clave + " no puede ir vacía");
            }
            if ("cleanup.policy".equals(clave) && !ReglasKafka.POLITICAS_DE_LIMPIEZA.contains(valor)) {
                throw new SolicitudInvalidaException("cleanup.policy debe ser " + ReglasKafka.POLITICAS_DE_LIMPIEZA);
            }
            if (!"cleanup.policy".equals(clave) && !valor.matches("-?\\d+")) {
                throw new SolicitudInvalidaException(clave + " debe ser un número (" + valor + ")");
            }
            validada.put(clave, valor);
        });
        return validada;
    }

    private static void exigirConfiguracionPermitida(String clave) {
        if (!ReglasKafka.CONFIGURACIONES.contains(clave)) {
            throw new SolicitudInvalidaException("La configuración " + clave + " no se administra desde aquí. "
                    + "Se permiten: " + new java.util.TreeSet<>(ReglasKafka.CONFIGURACIONES));
        }
    }

    private static void exigirDeLaPlataforma(String nombre) {
        if (nombre == null || !nombre.matches(ReglasKafka.PATRON)) {
            throw new SolicitudInvalidaException("Solo se administran tópicos de la plataforma (prefijo "
                    + ReglasKafka.PREFIJO + "): " + nombre);
        }
    }

    private static String estado(ConsumerGroupDescription descripcion) {
        return descripcion.groupState() == null ? "DESCONOCIDO" : descripcion.groupState().name();
    }

    /**
     * Espera la respuesta del clúster y traduce sus errores a los códigos de la API: lo que no
     * existe es 404, lo que choca con el estado es 409, lo inválido es 400 y sin respuesta, 503.
     */
    private static <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (TimeoutException error) {
            throw new KafkaNoDisponibleException("Kafka no respondió en " + ESPERA_SEGUNDOS
                    + " s. Revisa que los brokers estén arriba.", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Se interrumpió la espera a Kafka", error);
        } catch (ExecutionException error) {
            Throwable causa = error.getCause();
            String mensaje = causa.getMessage();
            if (causa instanceof UnknownTopicOrPartitionException || causa instanceof GroupIdNotFoundException) {
                throw new RecursoNoEncontradoException(mensaje);
            }
            if (causa instanceof TopicExistsException || causa instanceof GroupNotEmptyException
                    || causa instanceof UnknownMemberIdException) {
                throw new ConflictoDeEstadoException(mensaje);
            }
            if (causa instanceof InvalidReplicationFactorException || causa instanceof InvalidPartitionsException
                    || causa instanceof InvalidConfigurationException || causa instanceof PolicyViolationException
                    || causa instanceof InvalidRequestException) {
                throw new SolicitudInvalidaException(mensaje);
            }
            if (causa instanceof org.apache.kafka.common.errors.TimeoutException) {
                throw new KafkaNoDisponibleException("Kafka no respondió: " + mensaje, causa);
            }
            throw new KafkaNoDisponibleException("Kafka rechazó la operación: " + mensaje, causa);
        }
    }
}
