package com.pedidos360.pedidos.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba que la topología (exchanges, colas, DLQ y bindings) se arma según la configuración. */
class MensajeriaTopologiaTest {

    private final MensajeriaProperties props = new MensajeriaProperties(
            new MensajeriaProperties.Exchange("pedidos.exchange", "pedidos.dlx"),
            new MensajeriaProperties.ColaConBinding("cocina.comanda.q", "cocina.comanda.dlq", "pedido.creado.*"),
            new MensajeriaProperties.NotificacionCliente("cliente.notificacion.q", "cliente.notificacion.dlq",
                    "pedido.creado.*", "pedido.estado.*", "pedido.cancelado.*"),
            new MensajeriaProperties.ColaConBinding("alertas.stock.q", "alertas.stock.dlq", "stock.bajo.*"),
            new MensajeriaProperties.RoutingKey("pedido.creado", "pedido.estado", "pedido.cancelado"));

    private final ExchangesRabbitConfig exchanges = new ExchangesRabbitConfig();
    private final TopicExchange topic = exchanges.pedidosExchange(props);
    private final DirectExchange dlx = exchanges.dlxExchange(props);

    private void assertColaQuorumConDlq(Queue cola, String nombre, String dlq) {
        assertEquals(nombre, cola.getName());
        assertTrue(cola.isDurable());
        assertEquals("quorum", cola.getArguments().get("x-queue-type"));
        assertEquals("pedidos.dlx", cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals(dlq, cola.getArguments().get("x-dead-letter-routing-key"));
    }

    private void assertBinding(Binding binding, String exchange, String destino, String routingKey) {
        assertEquals(exchange, binding.getExchange());
        assertEquals(destino, binding.getDestination());
        assertEquals(routingKey, binding.getRoutingKey());
    }

    @Test
    void elExchangePrincipalEsTopicYElDlxEsDirect() {
        assertEquals("pedidos.exchange", topic.getName());
        assertTrue(topic.isDurable());
        assertEquals("pedidos.dlx", dlx.getName());
        assertTrue(dlx.isDurable());
    }

    @Test
    void comandaTieneColaDlqYBindings() {
        ComandaCocinaRabbitConfig config = new ComandaCocinaRabbitConfig(props);
        assertColaQuorumConDlq(config.comandaQueue(), "cocina.comanda.q", "cocina.comanda.dlq");
        assertEquals("cocina.comanda.dlq", config.comandaDlq().getName());
        assertBinding(config.comandaBinding(topic), "pedidos.exchange", "cocina.comanda.q", "pedido.creado.*");
        assertBinding(config.comandaDlqBinding(dlx), "pedidos.dlx", "cocina.comanda.dlq", "cocina.comanda.dlq");
    }

    @Test
    void notificacionClienteEscuchaCreacionEstadoYCancelacion() {
        NotificacionClienteRabbitConfig config = new NotificacionClienteRabbitConfig(props);
        assertColaQuorumConDlq(config.notificacionClienteQueue(), "cliente.notificacion.q", "cliente.notificacion.dlq");
        assertBinding(config.notificacionClienteBindingCreado(topic),
                "pedidos.exchange", "cliente.notificacion.q", "pedido.creado.*");
        assertBinding(config.notificacionClienteBindingEstado(topic),
                "pedidos.exchange", "cliente.notificacion.q", "pedido.estado.*");
        assertBinding(config.notificacionClienteBindingCancelado(topic),
                "pedidos.exchange", "cliente.notificacion.q", "pedido.cancelado.*");
        assertBinding(config.notificacionClienteDlqBinding(dlx),
                "pedidos.dlx", "cliente.notificacion.dlq", "cliente.notificacion.dlq");
    }

    @Test
    void alertaStockEscuchaStockBajo() {
        AlertaStockRabbitConfig config = new AlertaStockRabbitConfig(props);
        assertColaQuorumConDlq(config.alertaStockQueue(), "alertas.stock.q", "alertas.stock.dlq");
        assertBinding(config.alertaStockBinding(topic), "pedidos.exchange", "alertas.stock.q", "stock.bajo.*");
        assertBinding(config.alertaStockDlqBinding(dlx), "pedidos.dlx", "alertas.stock.dlq", "alertas.stock.dlq");
    }
}