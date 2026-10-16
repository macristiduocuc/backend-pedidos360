package com.pedidos360.inventario.messaging.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el convertidor crudo para los @RabbitListener (messageConverter = NOMBRE_BEAN).
 * No es candidato de autowiring a propósito: el convertidor por defecto de la aplicación
 * (el JSON que usa el productor) sigue siendo el único que Spring Boot asocia al RabbitTemplate.
 */
@Configuration
public class ConvertidorCrudoRabbitConfig {

    public static final String NOMBRE_BEAN = "convertidorCrudo";

    @Bean(name = NOMBRE_BEAN, autowireCandidate = false)
    public MessageConverter convertidorCrudo() {
        return new ConvertidorCrudo();
    }
}