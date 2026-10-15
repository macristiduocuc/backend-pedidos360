package com.pedidos360.inventario.event;

import java.time.Instant;

/**
 * Alerta de stock bajo. StockService la anuncia y AlertaStockPublisher la publica en RabbitMQ;
 * quien la consume es pedidos-service.
 * No lleva datos personales: solo datos del producto.
 *
 * @param stockMinimo el umbral configurado en application.yml (poco stock = menos que este valor)
 */
public record AlertaStockEvento(
        Long productoId,
        String nombreProducto,
        Long localId,
        Integer stockActual,
        Integer stockMinimo,
        Instant ocurridoEn) {
}