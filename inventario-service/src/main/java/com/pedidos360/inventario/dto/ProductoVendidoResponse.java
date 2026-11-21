package com.pedidos360.inventario.dto;

/** Un producto con las unidades que se han vendido (analítica calculada desde Kafka). */
public record ProductoVendidoResponse(Long productoId, String nombre, long unidades) {
}