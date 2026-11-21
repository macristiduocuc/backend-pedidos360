package com.pedidos360.inventario.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de los tópicos de Kafka (application.yml, prefijo "app.kafka.topicos").
 *
 * @param pedidosEventos tópico con el historial de eventos de los pedidos
 */
@ConfigurationProperties(prefix = "app.kafka.topicos")
public record KafkaTopicosProperties(String pedidosEventos) {
}