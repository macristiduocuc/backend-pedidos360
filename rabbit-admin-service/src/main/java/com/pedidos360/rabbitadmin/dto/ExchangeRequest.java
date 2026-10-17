package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos para crear un exchange.
 *
 * @param nombre  obligatorio; solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)
 * @param tipo    obligatorio: DIRECT, TOPIC, FANOUT o HEADERS
 * @param durable si sobrevive a un reinicio de RabbitMQ (por defecto: true)
 */
public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "El nombre solo puede tener letras, números, punto, guion y guion bajo (máximo 255 caracteres)")
        String nombre,
        @NotNull(message = "El tipo del exchange es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)")
        TipoExchange tipo,
        Boolean durable) {
}
