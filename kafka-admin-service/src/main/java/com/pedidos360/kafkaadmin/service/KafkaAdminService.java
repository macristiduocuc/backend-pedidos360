package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupResponse;
import com.pedidos360.kafkaadmin.dto.MiembroInfo;
import com.pedidos360.kafkaadmin.dto.ParticionInfo;
import com.pedidos360.kafkaadmin.dto.TopicRequest;
import com.pedidos360.kafkaadmin.dto.TopicResponse;
import com.pedidos360.kafkaadmin.exception.KafkaAdminException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Toda la lógica para administrar Kafka vive aquí: los controladores solo reciben la petición y
 * llaman a estos métodos, sin conocer la librería de Kafka.
 *
 * Reglas:
 *  - Los nombres solo admiten letras, números, punto, guion y guion bajo, y no pueden empezar con "__" (internos).
 *  - Solo se aceptan ciertas configuraciones, con valores válidos (ver ConfiguracionesPermitidas).
 *  - Los tópicos del sistema (application.yml) no se pueden eliminar.
 *  - Las particiones solo pueden aumentar.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,249}$");

    private final Admin admin;
    private final KafkaAdminProperties props;

    public KafkaAdminService(Admin admin, KafkaAdminProperties props) {
        this.admin = admin;
        this.props = props;
    }

    // ----------------------------------------------------------------- tópicos

    public TopicResponse crearTopico(TopicRequest solicitud) {
        String nombre = solicitud.nombre();
        validarNombre(nombre);
        int particiones = valorEntre(solicitud.particiones(), 1, 50, "particiones");
        int replicacion = valorEntre(solicitud.replicacion(), 1, 3, "replicación");
        Map<String, String> configuracion = solicitud.configuracion() == null ? Map.of() : solicitud.configuracion();
        ConfiguracionesPermitidas.validar(configuracion, replicacion);

        NewTopic nuevo = new NewTopic(nombre, particiones, (short) replicacion).configs(configuracion);
        esperar(admin.createTopics(List.of(nuevo)).all(), "crear el tópico '" + nombre + "'");
        log.info("Tópico '{}' creado ({} particiones, replicación {}, configuración {})",
                nombre, particiones, replicacion, configuracion);
        return new TopicResponse(nombre, particiones, replicacion, configuracion, List.of());
    }

    /** Lista los tópicos de negocio (oculta los internos de Kafka, que empiezan con "__"). */
    public List<String> listarTopicos() {
        Collection<String> nombres = esperar(admin.listTopics().names(), "listar los tópicos");
        return nombres.stream().filter(n -> !n.startsWith("__")).sorted().toList();
    }

    public TopicResponse obtenerTopico(String nombre) {
        validarNombreExistente(nombre);
        Map<String, TopicDescription> descripciones =
                esperar(admin.describeTopics(List.of(nombre)).allTopicNames(), "consultar el tópico '" + nombre + "'");
        TopicDescription descripcion = descripciones.get(nombre);
        if (descripcion == null) {
            throw KafkaAdminException.noEncontrado("El tópico '" + nombre + "' no existe");
        }

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Map<ConfigResource, Config> configs =
                esperar(admin.describeConfigs(List.of(recurso)).all(), "leer la configuración de '" + nombre + "'");
        Map<String, String> configuracion = new TreeMap<>();
        Config config = configs.get(recurso);
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                if (ConfiguracionesPermitidas.CLAVES.contains(entrada.name())) {
                    configuracion.put(entrada.name(), entrada.value());
                }
            }
        }

        List<ParticionInfo> detalle = descripcion.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new ParticionInfo(p.partition(),
                        p.leader() == null ? null : p.leader().id(),
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        int replicacion = detalle.isEmpty() ? 0 : detalle.get(0).replicas().size();
        return new TopicResponse(nombre, detalle.size(), replicacion, configuracion, detalle);
    }

    public TopicResponse modificarConfiguracion(String nombre, Map<String, String> cambios) {
        validarNombreExistente(nombre);
        if (cambios == null || cambios.isEmpty()) {
            throw KafkaAdminException.solicitudInvalida("Indica al menos una configuración para cambiar");
        }
        ConfiguracionesPermitidas.validar(cambios, 0);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Collection<AlterConfigOp> operaciones = cambios.entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .collect(java.util.stream.Collectors.toList());
        esperar(admin.incrementalAlterConfigs(Map.<ConfigResource, Collection<AlterConfigOp>>of(recurso, operaciones)).all(),
                "modificar la configuración de '" + nombre + "'");
        log.info("Configuración del tópico '{}' modificada: {}", nombre, cambios);
        return obtenerTopico(nombre);
    }

    public TopicResponse aumentarParticiones(String nombre, int total) {
        validarNombreExistente(nombre);
        valorEntre(total, 1, 50, "particiones");
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all(),
                "aumentar las particiones de '" + nombre + "'");
        log.info("Tópico '{}' ahora tiene {} particiones", nombre, total);
        return obtenerTopico(nombre);
    }

    public void eliminarTopico(String nombre) {
        validarNombreExistente(nombre);
        if (props.nombresProtegidos().contains(nombre)) {
            throw KafkaAdminException.conflicto(
                    "El tópico '" + nombre + "' es parte del sistema de pedidos y no se puede eliminar");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all(), "eliminar el tópico '" + nombre + "'");
        log.info("Tópico '{}' eliminado", nombre);
    }

    // ----------------------------------------------------------- consumer groups

    public List<String> listarGrupos() {
        return esperar(admin.listConsumerGroups().all(), "listar los consumer groups").stream()
                .map(grupo -> grupo.groupId())
                .sorted()
                .toList();
    }

    public ConsumerGroupResponse obtenerGrupo(String id) {
        if (id == null || id.isBlank()) {
            throw KafkaAdminException.solicitudInvalida("El id del grupo es obligatorio");
        }
        Map<String, ConsumerGroupDescription> descripciones =
                esperar(admin.describeConsumerGroups(List.of(id)).all(), "consultar el grupo '" + id + "'");
        ConsumerGroupDescription descripcion = descripciones.get(id);
        Map<TopicPartition, OffsetAndMetadata> confirmados = esperar(
                admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata(), "leer los offsets del grupo '" + id + "'");

        if (descripcion == null || (descripcion.members().isEmpty() && confirmados.isEmpty())) {
            throw KafkaAdminException.noEncontrado("El grupo '" + id + "' no existe o no tiene actividad");
        }

        List<MiembroInfo> detalle = new ArrayList<>();
        for (MemberDescription miembro : descripcion.members()) {
            List<String> particiones = miembro.assignment().topicPartitions().stream()
                    .map(tp -> tp.topic() + "-" + tp.partition())
                    .sorted()
                    .toList();
            detalle.add(new MiembroInfo(miembro.clientId(), miembro.host(), particiones));
        }
        return new ConsumerGroupResponse(id, String.valueOf(descripcion.groupState()),
                descripcion.members().size(), detalle, calcularLag(confirmados));
    }

    /** Lag = mensajes publicados que el grupo todavía no ha leído. */
    private long calcularLag(Map<TopicPartition, OffsetAndMetadata> confirmados) {
        if (confirmados.isEmpty()) {
            return 0;
        }
        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        confirmados.keySet().forEach(tp -> consulta.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResultInfo> finales = esperar(admin.listOffsets(consulta).all(), "calcular el lag");
        long lag = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : confirmados.entrySet()) {
            ListOffsetsResultInfo fin = finales.get(e.getKey());
            if (fin != null) {
                lag += Math.max(0, fin.offset() - e.getValue().offset());
            }
        }
        return lag;
    }

    // ------------------------------------------------------------------ ayudas

    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches() || nombre.equals(".") || nombre.equals("..")) {
            throw KafkaAdminException.solicitudInvalida("El nombre del tópico no es válido: usa solo letras, números, "
                    + "punto, guion y guion bajo (máximo 249 caracteres)");
        }
        if (nombre.startsWith("__")) {
            throw KafkaAdminException.solicitudInvalida("Los nombres que empiezan con '__' están reservados por Kafka");
        }
    }

    private void validarNombreExistente(String nombre) {
        validarNombre(nombre);
    }

    private int valorEntre(Integer valor, int minimo, int maximo, String campo) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw KafkaAdminException.solicitudInvalida("Las " + campo + " deben estar entre " + minimo + " y " + maximo);
        }
        return valor;
    }

    /** Espera la respuesta de Kafka y traduce sus errores a errores claros de la API. */
    private <T> T esperar(KafkaFuture<T> futuro, String descripcion) {
        try {
            return futuro.get(props.timeoutSegundos(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw KafkaAdminException.noDisponible("La operación fue interrumpida: no se pudo " + descripcion);
        } catch (java.util.concurrent.TimeoutException ex) {
            throw KafkaAdminException.noDisponible("Kafka no respondió a tiempo: no se pudo " + descripcion);
        } catch (ExecutionException ex) {
            throw traducir(ex.getCause() != null ? ex.getCause() : ex, descripcion);
        }
    }

    private KafkaAdminException traducir(Throwable causa, String descripcion) {
        if (causa instanceof TopicExistsException) {
            return KafkaAdminException.conflicto("El tópico ya existe; no se pudo " + descripcion);
        }
        if (causa instanceof UnknownTopicOrPartitionException) {
            return KafkaAdminException.noEncontrado("El tópico no existe; no se pudo " + descripcion);
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidPartitionsException || causa instanceof InvalidReplicationFactorException) {
            return KafkaAdminException.solicitudInvalida("Kafka rechazó los datos (" + causa.getMessage() + ")");
        }
        if (causa instanceof org.apache.kafka.common.errors.TimeoutException) {
            return KafkaAdminException.noDisponible("No hay conexión con Kafka; no se pudo " + descripcion);
        }
        log.error("Kafka rechazó la operación '{}': {}", descripcion, causa.toString());
        return KafkaAdminException.errorBroker("Kafka rechazó la operación: no se pudo " + descripcion);
    }
}
