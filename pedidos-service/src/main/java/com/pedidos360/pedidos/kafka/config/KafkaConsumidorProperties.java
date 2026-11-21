package com.pedidos360.pedidos.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Política de reintentos de los consumidores (application.yml, prefijo "app.kafka.consumidor").
 *
 * @param maxIntentos     cuántas veces se intenta procesar un mensaje antes de descartarlo
 * @param esperaReintento pausa entre un intento y el siguiente
 */
@ConfigurationProperties(prefix = "app.kafka.consumidor")
public record KafkaConsumidorProperties(int maxIntentos, Duration esperaReintento) {

    public KafkaConsumidorProperties {
        if (maxIntentos < 1) {
            maxIntentos = 1;
        }
        if (esperaReintento == null) {
            esperaReintento = Duration.ofSeconds(1);
        }
    }
}