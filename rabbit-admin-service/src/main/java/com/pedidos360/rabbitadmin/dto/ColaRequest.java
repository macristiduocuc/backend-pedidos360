package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para crear una cola.
 *
 * @param nombre               obligatorio; solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)
 * @param durable              si sobrevive a un reinicio de RabbitMQ (por defecto: true)
 * @param quorum               si es de tipo quorum, replicada entre los nodos del clúster (por defecto: true)
 * @param deadLetterExchange   exchange al que van los mensajes rechazados (opcional)
 * @param deadLetterRoutingKey routing key con la que se envían a ese exchange (opcional)
 */
public record ColaRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "El nombre solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)")
        String nombre,
        Boolean durable,
        Boolean quorum,
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "El exchange de mensajes fallidos solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)")
        String deadLetterExchange,
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "La routing key de mensajes fallidos solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)")
        String deadLetterRoutingKey) {
}
