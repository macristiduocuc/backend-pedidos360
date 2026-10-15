package com.pedidos360.pedidos.messaging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la lectura de la política de consumidores desde application.yml. */
@Configuration
@EnableConfigurationProperties(ConsumidorProperties.class)
public class ConsumidorRabbitConfig {
}