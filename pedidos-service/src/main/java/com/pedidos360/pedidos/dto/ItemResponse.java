package com.pedidos360.pedidos.dto;

import com.pedidos360.pedidos.model.PedidoItem;

import java.math.BigDecimal;

public record ItemResponse(
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario
) {
    public static ItemResponse desde(PedidoItem item) {
        return new ItemResponse(item.getProductoId(), item.getNombreProducto(), item.getCantidad(), item.getPrecioUnitario());
    }
}
