package com.pedidos360.pedidos.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Política de reintentos de los consumidores (application.yml, prefijo "app.mensajeria.consumidor").
 *
 * @param maxIntentos      cuántas veces se intenta procesar un mensaje antes de mandarlo a la DLQ
 * @param esperaReintento  pausa entre un intento y el siguiente
 */
@ConfigurationProperties(prefix = "app.mensajeria.consumidor")
public record ConsumidorProperties(int maxIntentos, Duration esperaReintento) {

    public ConsumidorProperties {
        if (maxIntentos < 1) {
            maxIntentos = 1;
        }
        if (esperaReintento == null) {
            esperaReintento = Duration.ZERO;
        }
    }
}