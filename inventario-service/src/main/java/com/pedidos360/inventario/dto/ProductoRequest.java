package com.pedidos360.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Lo que recibimos al crear/editar un producto desde el frontend. */
public record ProductoRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @PositiveOrZero BigDecimal precio,
        @NotNull @PositiveOrZero Integer stock,
        String imagenUrl,
        @NotNull Long categoriaId,
        @NotNull Long localId
) {}
