package com.pedidos360.inventario.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Comprueba que la topología (exchanges, cola de reposición, DLQ y bindings) se arma según la configuración. */
class MensajeriaTopologiaTest {

    private final MensajeriaProperties props = new MensajeriaProperties(
            new MensajeriaProperties.Exchange("pedidos.exchange", "pedidos.dlx"),
            new MensajeriaProperties.ColaConBinding("inventario.reposicion.q", "inventario.reposicion.dlq", "pedido.cancelado.*"),
            new MensajeriaProperties.RoutingKey("stock.bajo.local{localId}"));

    private final ExchangesRabbitConfig exchanges = new ExchangesRabbitConfig();
    private final TopicExchange topic = exchanges.pedidosExchange(props);
    private final DirectExchange dlx = exchanges.dlxExchange(props);
    private final ReposicionStockRabbitConfig reposicion = new ReposicionStockRabbitConfig(props);

    @Test
    void elExchangePrincipalEsTopicYElDlxEsDirect() {
        assertEquals("pedidos.exchange", topic.getName());
        assertTrue(topic.isDurable());
        assertEquals("pedidos.dlx", dlx.getName());
        assertTrue(dlx.isDurable());
    }

    @Test
    void laColaDeReposicionEsQuorumYTieneDlq() {
        Queue cola = reposicion.reposicionQueue();
        assertEquals("inventario.reposicion.q", cola.getName());
        assertTrue(cola.isDurable());
        assertEquals("quorum", cola.getArguments().get("x-queue-type"));
        assertEquals("pedidos.dlx", cola.getArguments().get("x-dead-letter-exchange"));
        assertEquals("inventario.reposicion.dlq", cola.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("inventario.reposicion.dlq", reposicion.reposicionDlq().getName());
    }

    @Test
    void laReposicionEscuchaLasCancelaciones() {
        Binding binding = reposicion.reposicionBinding(topic);
        assertEquals("pedidos.exchange", binding.getExchange());
        assertEquals("inventario.reposicion.q", binding.getDestination());
        assertEquals("pedido.cancelado.*", binding.getRoutingKey());

        Binding bindingDlq = reposicion.reposicionDlqBinding(dlx);
        assertEquals("pedidos.dlx", bindingDlq.getExchange());
        assertEquals("inventario.reposicion.dlq", bindingDlq.getDestination());
        assertEquals("inventario.reposicion.dlq", bindingDlq.getRoutingKey());
    }
}