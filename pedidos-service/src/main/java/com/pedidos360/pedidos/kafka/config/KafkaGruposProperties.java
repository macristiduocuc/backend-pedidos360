package com.pedidos360.pedidos.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de los consumer groups (application.yml, prefijo "app.kafka.grupos").
 *
 * @param auditoria grupo que guarda el historial de eventos de pedidos
 */
@ConfigurationProperties(prefix = "app.kafka.grupos")
public record KafkaGruposProperties(String auditoria) {
}