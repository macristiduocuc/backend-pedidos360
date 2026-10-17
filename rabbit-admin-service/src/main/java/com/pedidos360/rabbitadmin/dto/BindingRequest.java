package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para enlazar una cola a un exchange.
 *
 * @param exchange   exchange de origen (debe existir)
 * @param cola       cola de destino (debe existir)
 * @param routingKey patrón de enrutamiento (puede ir vacío, por ejemplo en un exchange fanout)
 */
public record BindingRequest(
        @NotBlank(message = "El exchange es obligatorio")
        @Size(max = 255, message = "El exchange no puede superar los 255 caracteres")
        String exchange,
        @NotBlank(message = "La cola es obligatoria")
        @Size(max = 255, message = "La cola no puede superar los 255 caracteres")
        String cola,
        @NotNull(message = "La routing key es obligatoria (puede ir vacía)")
        @Size(max = 255, message = "La routing key no puede superar los 255 caracteres")
        String routingKey) {
}
