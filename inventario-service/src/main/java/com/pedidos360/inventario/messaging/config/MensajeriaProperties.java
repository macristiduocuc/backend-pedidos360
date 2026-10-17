package com.pedidos360.inventario.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee de application.yml (prefijo "app.mensajeria") todos los nombres de la mensajería.
 * Es la única fuente de nombres de exchanges, colas y routing keys de este servicio.
 */
@ConfigurationProperties(prefix = "app.mensajeria")
public record MensajeriaProperties(
        Exchange exchange,
        ColaConBinding reposicion,
        RoutingKey routingKey) {

    /** Exchange principal (topic) y exchange de mensajes fallidos (direct). */
    public record Exchange(String principal, String dlx) {
    }

    /** Una cola, su DLQ y el patrón de binding con el que se enlaza al exchange principal. */
    public record ColaConBinding(String cola, String dlq, String binding) {
    }

    /** Routing keys que publica este microservicio. */
    public record RoutingKey(String stockBajo) {
    }
}