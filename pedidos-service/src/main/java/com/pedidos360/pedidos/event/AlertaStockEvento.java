package com.pedidos360.pedidos.event;

import java.time.Instant;

/**
 * Mensaje de alerta de stock bajo. Lo publica inventario-service y lo consume este servicio.
 * No lleva datos personales: solo datos del producto.
 */
public record AlertaStockEvento(
        Long productoId,
        String nombreProducto,
        Long localId,
        Integer stockActual,
        Integer stockMinimo,
        Instant ocurridoEn) {
}