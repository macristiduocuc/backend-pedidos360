package com.pedidos360.inventario.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de los consumer groups (application.yml, prefijo "app.kafka.grupos").
 *
 * @param analiticaVentas grupo que calcula las unidades vendidas por producto
 */
@ConfigurationProperties(prefix = "app.kafka.grupos")
public record KafkaGruposProperties(String analiticaVentas) {
}