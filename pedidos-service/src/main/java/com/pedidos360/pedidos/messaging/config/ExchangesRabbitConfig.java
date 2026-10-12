package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchanges compartidos por todos los casos de uso:
 * - pedidos.exchange (topic): por aquí viajan los eventos de negocio.
 * - pedidos.dlx (direct): recibe los mensajes fallidos y los enruta a su DLQ.
 */
@Configuration
@EnableConfigurationProperties(MensajeriaProperties.class)
public class ExchangesRabbitConfig {

    @Bean
    public TopicExchange pedidosExchange(MensajeriaProperties props) {
        return new TopicExchange(props.exchange().principal(), true, false);
    }

    @Bean
    public DirectExchange dlxExchange(MensajeriaProperties props) {
        return new DirectExchange(props.exchange().dlx(), true, false);
    }
}