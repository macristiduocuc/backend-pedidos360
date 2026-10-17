package com.pedidos360.inventario.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento de pedido que publica pedidos-service. Este servicio solo lo consume cuando el
 * pedido fue CANCELADO, para devolver el stock. Tiene la misma forma (JSON) que el mensaje:
 * no lleva datos personales, solo ids, estado, total, hora e ítems.
 */
public record PedidoEvento(
        Tipo tipo,
        Long pedidoId,
        Long localId,
        String estado,
        BigDecimal total,
        Instant ocurridoEn,
        List<Item> items) {

    public enum Tipo {
        CREADO,
        ESTADO_CAMBIADO,
        CANCELADO
    }

    /** Producto y cantidad del pedido. */
    public record Item(Long productoId, Integer cantidad) {
    }
}