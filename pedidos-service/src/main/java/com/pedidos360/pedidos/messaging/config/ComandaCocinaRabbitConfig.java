package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso: ticket de comanda para cocina.
 * Ruta: pedidos.exchange --(pedido.creado.*)--> cocina.comanda.q --(falla)--> cocina.comanda.dlq
 */
@Configuration
public class ComandaCocinaRabbitConfig {

    private final MensajeriaProperties.ColaConBinding caso;
    private final String dlx;

    public ComandaCocinaRabbitConfig(MensajeriaProperties props) {
        this.caso = props.comanda();
        this.dlx = props.exchange().dlx();
    }

    @Bean
    public Queue comandaQueue() {
        return ColasRabbit.colaPrincipal(caso.cola(), dlx, caso.dlq());
    }

    @Bean
    public Queue comandaDlq() {
        return ColasRabbit.colaDlq(caso.dlq());
    }

    @Bean
    public Binding comandaBinding(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(comandaQueue()).to(pedidosExchange).with(caso.binding());
    }

    @Bean
    public Binding comandaDlqBinding(DirectExchange dlxExchange) {
        return ColasRabbit.bindingDlq(comandaDlq(), dlxExchange);
    }
}