package com.pedidos360.pedidos.event;

import java.time.Instant;

/**
 * Stock actual de un producto, publicado por inventario-service en el tópico inventario.stock.
 * Tiene la misma forma (JSON) que el mensaje; no lleva datos personales.
 */
public record StockActualizadoEvento(
        Long productoId,
        String nombreProducto,
        Long localId,
        Integer stock,
        Instant ocurridoEn) {
}