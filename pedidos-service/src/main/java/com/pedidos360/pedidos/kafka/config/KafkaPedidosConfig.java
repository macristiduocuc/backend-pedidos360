package com.pedidos360.pedidos.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la lectura de los nombres de tópicos desde application.yml. */
@Configuration
@EnableConfigurationProperties(KafkaTopicosProperties.class)
public class KafkaPedidosConfig {
}