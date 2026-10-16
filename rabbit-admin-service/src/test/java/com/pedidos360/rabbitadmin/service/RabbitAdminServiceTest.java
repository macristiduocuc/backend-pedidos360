package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.BindingResponse;
import com.pedidos360.rabbitadmin.dto.ColaRequest;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.dto.ExchangeRequest;
import com.pedidos360.rabbitadmin.dto.TipoExchange;
import com.pedidos360.rabbitadmin.exception.RabbitAdminException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba las reglas del administrador: validaciones, recursos protegidos y traducción de errores. */
class RabbitAdminServiceTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final RabbitAdminProperties props = new RabbitAdminProperties(new RabbitAdminProperties.Protegidos(
            List.of("pedidos.exchange", "pedidos.dlx"),
            List.of("cocina.comanda.q", "cocina.comanda.dlq")));
    private final RabbitAdminService servicio = new RabbitAdminService(amqpAdmin, props);

    private int estadoDe(Executable accion) {
        return assertThrows(RabbitAdminException.class, accion).getEstado().value();
    }

    // ------------------------------------------------------------------ colas

    @Test
    void creaUnaColaQuorumDurableConSuDlqPorDefecto() {
        when(amqpAdmin.getQueueInfo("demo.q")).thenReturn(new QueueInformation("demo.q", 0, 0));

        ColaResponse respuesta = servicio.crearCola(
                new ColaRequest("demo.q", null, null, "pedidos.dlx", "demo.dlq"));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        Queue cola = captor.getValue();
        assertEquals("demo.q", cola.getName());
        assertTrue(cola.isDurable());
        assertEquals("quorum", cola.getArguments().get("x-queue-type"));
        assertEquals("pedidos.dlx", cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals("demo.dlq", cola.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("demo.q", respuesta.nombre());
    }

    @Test
    void creaUnaColaClasicaNoDurableSiSeLoPiden() {
        when(amqpAdmin.getQueueInfo("demo.q")).thenReturn(new QueueInformation("demo.q", 0, 0));

        servicio.crearCola(new ColaRequest("demo.q", false, false, null, null));

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(captor.capture());
        assertFalse(captor.getValue().isDurable());
        assertFalse(captor.getValue().getArguments().containsKey("x-queue-type"));
    }

    @Test
    void rechazaNombresDeColaInvalidos() {
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest("mi cola", null, null, null, null))));
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest("", null, null, null, null))));
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest(null, null, null, null, null))));
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void rechazaNombresReservadosPorRabbitMq() {
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest("amq.prueba", null, null, null, null))));
    }

    @Test
    void rechazaUnaColaQuorumNoDurable() {
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest("demo.q", false, true, null, null))));
    }

    @Test
    void rechazaUnaRoutingKeyDeFallidosSinExchangeDeFallidos() {
        assertEquals(400, estadoDe(() -> servicio.crearCola(new ColaRequest("demo.q", null, null, null, "demo.dlq"))));
    }

    @Test
    void noPermiteEliminarUnaColaProtegida() {
        assertEquals(409, estadoDe(() -> servicio.eliminarCola("cocina.comanda.q")));
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void eliminaUnaColaNoProtegida() {
        servicio.eliminarCola("demo.q");

        verify(amqpAdmin).deleteQueue("demo.q");
    }

    @Test
    void consultarUnaColaInexistenteDevuelve404() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);

        assertEquals(404, estadoDe(() -> servicio.obtenerCola("no.existe")));
    }

    // -------------------------------------------------------------- exchanges

    @Test
    void creaUnExchangeTopicDurable() {
        servicio.crearExchange(new ExchangeRequest("demo.ex", TipoExchange.TOPIC, null));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
        verify(amqpAdmin).declareExchange(captor.capture());
        assertInstanceOf(TopicExchange.class, captor.getValue());
        assertEquals("demo.ex", captor.getValue().getName());
        assertTrue(captor.getValue().isDurable());
    }

    @Test
    void noPermiteEliminarUnExchangeProtegido() {
        assertEquals(409, estadoDe(() -> servicio.eliminarExchange("pedidos.exchange")));
        verify(amqpAdmin, never()).deleteExchange(anyString());
    }

    // --------------------------------------------------------------- bindings

    @Test
    void noCreaUnBindingSiLaColaNoExiste() {
        when(amqpAdmin.getQueueInfo("no.existe")).thenReturn(null);

        assertEquals(404, estadoDe(() -> servicio.crearBinding(
                new BindingRequest("pedidos.exchange", "no.existe", "demo.#"))));
        verify(amqpAdmin, never()).declareBinding(any(Binding.class));
    }

    @Test
    void creaUnBindingEntreUnExchangeYUnaCola() {
        when(amqpAdmin.getQueueInfo("demo.q")).thenReturn(new QueueInformation("demo.q", 0, 0));

        BindingResponse respuesta = servicio.crearBinding(new BindingRequest("pedidos.exchange", "demo.q", "demo.#"));

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());
        assertEquals("pedidos.exchange", captor.getValue().getExchange());
        assertEquals("demo.q", captor.getValue().getDestination());
        assertEquals("demo.#", captor.getValue().getRoutingKey());
        assertEquals("demo.#", respuesta.routingKey());
    }

    @Test
    void noPermiteEliminarUnBindingDelSistema() {
        assertEquals(409, estadoDe(() -> servicio.eliminarBinding(
                "pedidos.exchange", "cocina.comanda.q", "pedido.creado.*")));
        verify(amqpAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void eliminaUnBindingPropioAunqueElExchangeSeaDelSistema() {
        servicio.eliminarBinding("pedidos.exchange", "demo.q", "demo.#");

        verify(amqpAdmin).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarUnBindingSinExchangeEsUnaSolicitudInvalida() {
        assertEquals(400, estadoDe(() -> servicio.eliminarBinding("", "demo.q", "demo.#")));
    }

    // ----------------------------------------------------- errores de RabbitMQ

    @Test
    void siRabbitMqNoRespondeDevuelve503() {
        when(amqpAdmin.declareQueue(any(Queue.class)))
                .thenThrow(new AmqpConnectException(new ConnectException("conexión rechazada")));

        assertEquals(503, estadoDe(() -> servicio.crearCola(new ColaRequest("demo.q", null, null, null, null))));
    }

    @Test
    void siLaColaYaExisteConOtraConfiguracionDevuelve409() {
        when(amqpAdmin.declareQueue(any(Queue.class)))
                .thenThrow(new AmqpIOException(new IOException("PRECONDITION_FAILED - inequivalent arg 'x-queue-type'")));

        assertEquals(409, estadoDe(() -> servicio.crearCola(new ColaRequest("demo.q", null, null, null, null))));
    }
}
