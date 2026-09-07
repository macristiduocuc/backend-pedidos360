package com.pedidos360.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservaStockRequest(
        @NotNull @Positive Integer cantidad
) {}
