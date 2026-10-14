package com.pedidos360.pedidos.messaging.publisher;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.config.MensajeriaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PedidoEventoPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    private final MensajeriaProperties props = new MensajeriaProperties(
            new MensajeriaProperties.Exchange("pedidos.exchange", "pedidos.dlx"),
            new MensajeriaProperties.ColaConBinding("cocina.comanda.q", "cocina.comanda.dlq", "pedido.creado.*"),
            new MensajeriaProperties.NotificacionCliente("cliente.notificacion.q", "cliente.notificacion.dlq",
                    "pedido.creado.*", "pedido.estado.*", "pedido.cancelado.*"),
            new MensajeriaProperties.ColaConBinding("alertas.stock.q", "alertas.stock.dlq", "stock.bajo.*"),
            new MensajeriaProperties.RoutingKey("pedido.creado.local{localId}",
                    "pedido.estado.local{localId}", "pedido.cancelado.local{localId}"));

    private final PedidoEventoPublisher publisher = new PedidoEventoPublisher(rabbitTemplate, props);

    private PedidoEvento evento(PedidoEvento.Tipo tipo) {
        return new PedidoEvento(tipo, 7L, 3L, "Pendiente", new BigDecimal("1990.00"),
                Instant.parse("2026-10-12T10:00:00Z"), List.of(new PedidoEvento.Item(11L, 2)));
    }

    @Test
    void publicaElPedidoCreadoConLaRoutingKeyDelLocal() {
        PedidoEvento evento = evento(PedidoEvento.Tipo.CREADO);
        publisher.alOcurrirEvento(evento);
        verify(rabbitTemplate).convertAndSend("pedidos.exchange", "pedido.creado.local3", evento);
    }

    @Test
    void publicaElCambioDeEstadoConLaRoutingKeyDelLocal() {
        PedidoEvento evento = evento(PedidoEvento.Tipo.ESTADO_CAMBIADO);
        publisher.alOcurrirEvento(evento);
        verify(rabbitTemplate).convertAndSend("pedidos.exchange", "pedido.estado.local3", evento);
    }

    @Test
    void publicaLaCancelacionConLaRoutingKeyDelLocal() {
        PedidoEvento evento = evento(PedidoEvento.Tipo.CANCELADO);
        publisher.alOcurrirEvento(evento);
        verify(rabbitTemplate).convertAndSend("pedidos.exchange", "pedido.cancelado.local3", evento);
    }

    @Test
    void siRabbitFallaNoSePropagaElErrorYElPedidoNoSeAfecta() {
        doThrow(new AmqpException("RabbitMQ caído"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PedidoEvento.class));
        assertDoesNotThrow(() -> publisher.alOcurrirEvento(evento(PedidoEvento.Tipo.CREADO)));
    }

    @Test
    void elEventoNoTraeDatosPersonales() {
        List<String> campos = Arrays.stream(PedidoEvento.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertEquals(List.of("tipo", "pedidoId", "localId", "estado", "total", "ocurridoEn", "items"), campos);
    }
}