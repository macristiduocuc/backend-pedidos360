package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nuevo total de particiones. Kafka solo permite aumentar, nunca reducir. */
public record ParticionesRequest(
        @NotNull(message = "El total de particiones es obligatorio")
        @Min(value = 1, message = "Debe haber al menos 1 partición")
        @Max(value = 50, message = "No se permiten más de 50 particiones")
        Integer total) {
}
