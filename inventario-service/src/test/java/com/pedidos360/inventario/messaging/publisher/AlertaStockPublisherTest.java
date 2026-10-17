package com.pedidos360.inventario.messaging.publisher;

import com.pedidos360.inventario.event.AlertaStockEvento;
import com.pedidos360.inventario.messaging.config.MensajeriaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.reflect.RecordComponent;
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

class AlertaStockPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    private final MensajeriaProperties props = new MensajeriaProperties(
            new MensajeriaProperties.Exchange("pedidos.exchange", "pedidos.dlx"),
            new MensajeriaProperties.ColaConBinding("inventario.reposicion.q", "inventario.reposicion.dlq", "pedido.cancelado.*"),
            new MensajeriaProperties.RoutingKey("stock.bajo.local{localId}"));

    private final AlertaStockPublisher publisher = new AlertaStockPublisher(rabbitTemplate, props);

    private AlertaStockEvento alerta() {
        return new AlertaStockEvento(1L, "Empanada de pino", 3L, 9, 10, Instant.parse("2026-10-14T12:00:00Z"));
    }

    @Test
    void publicaLaAlertaConLaRoutingKeyDelLocal() {
        AlertaStockEvento evento = alerta();

        publisher.alOcurrirAlerta(evento);

        verify(rabbitTemplate).convertAndSend("pedidos.exchange", "stock.bajo.local3", evento);
    }

    @Test
    void siRabbitFallaNoSePropagaElErrorYLaReservaNoSeAfecta() {
        doThrow(new AmqpException("RabbitMQ caído"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AlertaStockEvento.class));

        assertDoesNotThrow(() -> publisher.alOcurrirAlerta(alerta()));
    }

    @Test
    void laAlertaNoTraeDatosPersonales() {
        List<String> campos = Arrays.stream(AlertaStockEvento.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertEquals(List.of("productoId", "nombreProducto", "localId", "stockActual", "stockMinimo", "ocurridoEn"), campos);
    }
}