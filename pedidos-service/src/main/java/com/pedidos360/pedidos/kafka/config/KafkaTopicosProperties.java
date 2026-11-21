package com.pedidos360.pedidos.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de los tópicos de Kafka (application.yml, prefijo "app.kafka.topicos").
 * Es la única fuente de nombres de tópicos de este servicio.
 *
 * @param pedidosEventos  tópico con el historial de eventos de los pedidos (lo publica este servicio)
 * @param inventarioStock tópico con el último stock de cada producto (lo publica inventario-service)
 */
@ConfigurationProperties(prefix = "app.kafka.topicos")
public record KafkaTopicosProperties(String pedidosEventos, String inventarioStock) {
}