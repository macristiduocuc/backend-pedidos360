package com.pedidos360.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemRequest(
        @NotNull Long productoId,
        @NotNull @Positive Integer cantidad
) {}
