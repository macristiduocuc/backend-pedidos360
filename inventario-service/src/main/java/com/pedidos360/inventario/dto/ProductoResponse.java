package com.pedidos360.inventario.dto;

import com.pedidos360.inventario.model.Producto;

import java.math.BigDecimal;

/** Lo que exponemos al frontend: nunca la entidad JPA directa (evita problemas de lazy-loading al serializar). */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        String categoria,
        String local,
        Boolean activo
) {
    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock(),
                p.getImagenUrl(),
                p.getCategoria().getNombre(),
                p.getLocal().getNombre(),
                p.getActivo()
        );
    }
}
