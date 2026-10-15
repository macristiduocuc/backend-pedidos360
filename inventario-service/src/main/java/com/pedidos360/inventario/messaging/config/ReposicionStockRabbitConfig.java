package com.pedidos360.inventario.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso: reponer stock cuando se cancela un pedido.
 * Ruta: pedidos.exchange --(pedido.cancelado.*)--> inventario.reposicion.q --(falla)--> inventario.reposicion.dlq
 */
@Configuration
public class ReposicionStockRabbitConfig {

    private final MensajeriaProperties.ColaConBinding caso;
    private final String dlx;

    public ReposicionStockRabbitConfig(MensajeriaProperties props) {
        this.caso = props.reposicion();
        this.dlx = props.exchange().dlx();
    }

    @Bean
    public Queue reposicionQueue() {
        return ColasRabbit.colaPrincipal(caso.cola(), dlx, caso.dlq());
    }

    @Bean
    public Queue reposicionDlq() {
        return ColasRabbit.colaDlq(caso.dlq());
    }

    @Bean
    public Binding reposicionBinding(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(reposicionQueue()).to(pedidosExchange).with(caso.binding());
    }

    @Bean
    public Binding reposicionDlqBinding(DirectExchange dlxExchange) {
        return ColasRabbit.bindingDlq(reposicionDlq(), dlxExchange);
    }
}