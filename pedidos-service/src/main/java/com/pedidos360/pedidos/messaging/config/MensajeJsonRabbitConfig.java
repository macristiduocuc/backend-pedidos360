package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los mensajes viajan como JSON. Spring Boot asocia este convertidor, de forma
 * automática, al RabbitTemplate (productores) y a los listeners (consumidores).
 * Usa Jackson 3, que es el que corresponde a Spring Boot 4.
 */
@Configuration
public class MensajeJsonRabbitConfig {

    @Bean
    public MessageConverter mensajeJsonConverter() {
        return new JacksonJsonMessageConverter();
    }
}