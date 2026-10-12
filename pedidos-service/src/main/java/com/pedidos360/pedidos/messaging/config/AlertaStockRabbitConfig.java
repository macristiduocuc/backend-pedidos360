package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caso de uso: alerta de stock bajo (la publica inventario-service, la consume este servicio).
 * Ruta: pedidos.exchange --(stock.bajo.*)--> alertas.stock.q --(falla)--> alertas.stock.dlq
 */
@Configuration
public class AlertaStockRabbitConfig {

    private final MensajeriaProperties.ColaConBinding caso;
    private final String dlx;

    public AlertaStockRabbitConfig(MensajeriaProperties props) {
        this.caso = props.alertaStock();
        this.dlx = props.exchange().dlx();
    }

    @Bean
    public Queue alertaStockQueue() {
        return ColasRabbit.colaPrincipal(caso.cola(), dlx, caso.dlq());
    }

    @Bean
    public Queue alertaStockDlq() {
        return ColasRabbit.colaDlq(caso.dlq());
    }

    @Bean
    public Binding alertaStockBinding(TopicExchange pedidosExchange) {
        return BindingBuilder.bind(alertaStockQueue()).to(pedidosExchange).with(caso.binding());
    }

    @Bean
    public Binding alertaStockDlqBinding(DirectExchange dlxExchange) {
        return ColasRabbit.bindingDlq(alertaStockDlq(), dlxExchange);
    }
}