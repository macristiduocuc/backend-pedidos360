package com.pedidos360.inventario.event;

import java.time.Instant;

/**
 * Foto del stock de un producto, que inventario-service publica cada vez que el stock cambia
 * (compra, reposición por cancelación o edición de Auditoría). Es el mensaje del tópico inventario.stock,
 * que está "compactado": de cada producto Kafka conserva solo el valor más reciente.
 * No lleva datos personales: solo datos del producto.
 */
public record StockActualizadoEvento(
        Long productoId,
        String nombreProducto,
        Long localId,
        Integer stock,
        Instant ocurridoEn) {
}