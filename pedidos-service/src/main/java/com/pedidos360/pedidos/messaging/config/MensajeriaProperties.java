package com.pedidos360.pedidos.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee de application.yml (prefijo "app.mensajeria") todos los nombres de la
 * mensajería. Es la única fuente de nombres de exchanges, colas y routing keys.
 */
@ConfigurationProperties(prefix = "app.mensajeria")
public record MensajeriaProperties(
        Exchange exchange,
        ColaConBinding comanda,
        NotificacionCliente notificacionCliente,
        ColaConBinding alertaStock,
        RoutingKey routingKey) {

    /** Exchange principal (topic) y exchange de mensajes fallidos (direct). */
    public record Exchange(String principal, String dlx) {
    }

    /** Una cola, su DLQ y el patrón de binding con el que se enlaza al exchange principal. */
    public record ColaConBinding(String cola, String dlq, String binding) {
    }

    /** La cola de notificaciones al cliente recibe tres tipos de evento. */
    public record NotificacionCliente(String cola, String dlq,
                                      String bindingCreado, String bindingEstado, String bindingCancelado) {
    }

    /** Prefijos de las routing keys que publica este microservicio. */
    public record RoutingKey(String pedidoCreado, String pedidoEstado, String pedidoCancelado) {
    }
}