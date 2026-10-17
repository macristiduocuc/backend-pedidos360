package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso: notificación al cliente.
 * Ruta: pedidos.exchange --(pedido.creado.* | pedido.estado.* | pedido.cancelado.*)-->
 *       cliente.notificacion.q --(falla)--> cliente.notificacion.dlq
 */
@Configuration
public class NotificacionClienteRabbitConfig {

    private final MensajeriaProperties.NotificacionCliente caso;
    private final String dlx;

    public NotificacionClienteRabbitConfig(MensajeriaProperties props) {
        this.caso = props.notificacionCliente();
        this.dlx = props.exchange().dlx();
    }

    @Bean
    public Queue notificacionClienteQueue() {
        return ColasRabbit.colaPrincipal(caso.cola(), dlx, caso.dlq());
    }

    @Bean
    public Queue notificacionClienteDlq() {
        return ColasRabbit.colaDlq(caso.dlq());
    }

    @Bean
    public Binding notificacionClienteBindingCreado(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionClienteQueue()).to(pedidosExchange).with(caso.bindingCreado());
    }

    @Bean
    public Binding notificacionClienteBindingEstado(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionClienteQueue()).to(pedidosExchange).with(caso.bindingEstado());
    }

    @Bean
    public Binding notificacionClienteBindingCancelado(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionClienteQueue()).to(pedidosExchange).with(caso.bindingCancelado());
    }

    @Bean
    public Binding notificacionClienteDlqBinding(DirectExchange dlxExchange) {
        return ColasRabbit.bindingDlq(notificacionClienteDlq(), dlxExchange);
    }
}