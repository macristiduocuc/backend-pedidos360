package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.dto.TopicRequest;
import com.pedidos360.kafkaadmin.dto.TopicResponse;
import com.pedidos360.kafkaadmin.exception.KafkaAdminException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba las reglas del administrador: validaciones, tópicos protegidos y traducción de errores. */
class KafkaAdminServiceTest {

    private final Admin admin = mock(Admin.class);
    private final KafkaAdminProperties props = new KafkaAdminProperties(5, List.of(
            new KafkaAdminProperties.TopicoSistema("pedidos.eventos", 3, 2, Map.of("cleanup.policy", "delete")),
            new KafkaAdminProperties.TopicoSistema("inventario.stock", 3, 2, Map.of("cleanup.policy", "compact"))));
    private final KafkaAdminService servicio = new KafkaAdminService(admin, props);

    private int estadoDe(Executable accion) {
        return assertThrows(KafkaAdminException.class, accion).getEstado().value();
    }

    private TopicRequest solicitud(String nombre, Integer particiones, Integer replicacion, Map<String, String> config) {
        return new TopicRequest(nombre, particiones, replicacion, config);
    }

    private void creacionExitosa() {
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(anyCollection())).thenReturn(resultado);
    }

    private void creacionFallida(Throwable error) {
        KafkaFutureImpl<Void> futuro = new KafkaFutureImpl<>();
        futuro.completeExceptionally(error);
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(futuro);
        when(admin.createTopics(anyCollection())).thenReturn(resultado);
    }

    // ----------------------------------------------------------------- crear

    @Test
    @SuppressWarnings("unchecked")
    void creaUnTopicoConSusParticionesReplicacionYConfiguracion() {
        creacionExitosa();

        TopicResponse respuesta = servicio.crearTopico(
                solicitud("demo.admin", 3, 2, Map.of("cleanup.policy", "delete", "retention.ms", "3600000")));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        NewTopic creado = captor.getValue().iterator().next();
        assertEquals("demo.admin", creado.name());
        assertEquals(3, creado.numPartitions());
        assertEquals(2, creado.replicationFactor());
        assertEquals("delete", creado.configs().get("cleanup.policy"));
        assertEquals("3600000", creado.configs().get("retention.ms"));
        assertEquals("demo.admin", respuesta.nombre());
    }

    @Test
    void rechazaNombresInvalidos() {
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("mi topico", 3, 2, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("", 3, 2, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("..", 3, 2, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("__interno", 3, 2, null))));
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    void rechazaParticionesOReplicacionFueraDeRango() {
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 0, 2, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 51, 2, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 0, null))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 4, null))));
    }

    @Test
    void rechazaConfiguracionesNoPermitidasOConValoresInvalidos() {
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, Map.of("compression.type", "gzip")))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, Map.of("cleanup.policy", "otra")))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, Map.of("retention.ms", "abc")))));
        assertEquals(400, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, Map.of("min.insync.replicas", "5")))));
    }

    @Test
    void siElTopicoYaExisteResponde409() {
        creacionFallida(new TopicExistsException("ya existe"));

        assertEquals(409, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, null))));
    }

    @Test
    void siKafkaNoRespondeResponde503() {
        creacionFallida(new TimeoutException("sin respuesta"));

        assertEquals(503, estadoDe(() -> servicio.crearTopico(solicitud("demo", 3, 2, null))));
    }

    // ------------------------------------------------------------- modificar

    @Test
    void modificarConfiguracionValidaLosValores() {
        assertEquals(400, estadoDe(() -> servicio.modificarConfiguracion("demo", Map.of("retention.ms", "-5"))));
        assertEquals(400, estadoDe(() -> servicio.modificarConfiguracion("demo", Map.of("log.dirs", "/tmp"))));
        assertEquals(400, estadoDe(() -> servicio.modificarConfiguracion("demo", Map.of())));
    }

    @Test
    void noSePuedenReducirLasParticiones() {
        KafkaFutureImpl<Void> futuro = new KafkaFutureImpl<>();
        futuro.completeExceptionally(new InvalidPartitionsException("no se pueden reducir"));
        org.apache.kafka.clients.admin.CreatePartitionsResult resultado =
                mock(org.apache.kafka.clients.admin.CreatePartitionsResult.class);
        when(resultado.all()).thenReturn(futuro);
        when(admin.createPartitions(org.mockito.ArgumentMatchers.anyMap())).thenReturn(resultado);

        assertEquals(400, estadoDe(() -> servicio.aumentarParticiones("demo", 2)));
    }

    // -------------------------------------------------------------- eliminar

    @Test
    void noPermiteEliminarUnTopicoDelSistema() {
        assertEquals(409, estadoDe(() -> servicio.eliminarTopico("pedidos.eventos")));
        assertEquals(409, estadoDe(() -> servicio.eliminarTopico("inventario.stock")));
        verify(admin, never()).deleteTopics(anyCollection());
    }

    @Test
    void eliminaUnTopicoQueNoEsDelSistema() {
        DeleteTopicsResult resultado = mock(DeleteTopicsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.deleteTopics(anyCollection())).thenReturn(resultado);

        servicio.eliminarTopico("demo.admin");

        verify(admin).deleteTopics(anyCollection());
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listarOcultaLosTopicosInternosYOrdenaPorNombre() {
        ListTopicsResult resultado = mock(ListTopicsResult.class);
        when(resultado.names()).thenReturn(KafkaFuture.completedFuture(Set.of("zeta", "alfa", "__consumer_offsets")));
        when(admin.listTopics()).thenReturn(resultado);

        assertEquals(List.of("alfa", "zeta"), servicio.listarTopicos());
    }

    // -------------------------------------------------------------- describir

    @Test
    void describirMuestraParticionesLiderYConfiguracion() {
        Node broker1 = new Node(1, "kafka-1", 29092);
        Node broker2 = new Node(2, "kafka-2", 29093);
        TopicDescription descripcion = new TopicDescription("demo", false, List.of(
                new TopicPartitionInfo(0, broker1, List.of(broker1, broker2), List.of(broker1, broker2)),
                new TopicPartitionInfo(1, broker2, List.of(broker2, broker1), List.of(broker2, broker1))));
        DescribeTopicsResult topicos = mock(DescribeTopicsResult.class);
        when(topicos.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("demo", descripcion)));
        when(admin.describeTopics(anyCollection())).thenReturn(topicos);

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, "demo");
        DescribeConfigsResult configs = mock(DescribeConfigsResult.class);
        when(configs.all()).thenReturn(KafkaFuture.completedFuture(Map.of(recurso, new Config(List.of(
                new ConfigEntry("cleanup.policy", "compact"),
                new ConfigEntry("segment.bytes", "1073741824"))))));
        when(admin.describeConfigs(anyCollection())).thenReturn(configs);

        TopicResponse respuesta = servicio.obtenerTopico("demo");

        assertEquals(2, respuesta.particiones());
        assertEquals(2, respuesta.replicacion());
        assertEquals(1, respuesta.detalle().get(0).lider());
        assertEquals(2, respuesta.detalle().get(1).lider());
        assertEquals("compact", respuesta.configuracion().get("cleanup.policy"));
        assertTrue(!respuesta.configuracion().containsKey("segment.bytes"));
    }
}
