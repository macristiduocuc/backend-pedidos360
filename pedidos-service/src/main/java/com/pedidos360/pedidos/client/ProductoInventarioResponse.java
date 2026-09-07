package com.pedidos360.pedidos.client;

import java.math.BigDecimal;

/** Espejo del ProductoResponse que expone el microservicio de Inventario (GET /api/productos/{id}). */
public record ProductoInventarioResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        String categoria,
        String local,
        Boolean activo
) {}
