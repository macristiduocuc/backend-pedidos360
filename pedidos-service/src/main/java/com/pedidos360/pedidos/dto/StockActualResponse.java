package com.pedidos360.pedidos.dto;

import java.time.OffsetDateTime;

/** Último stock conocido de un producto. */
public record StockActualResponse(
        Long productoId,
        String nombreProducto,
        Long localId,
        int stock,
        OffsetDateTime ocurridoEn,
        OffsetDateTime actualizadoEn) {
}