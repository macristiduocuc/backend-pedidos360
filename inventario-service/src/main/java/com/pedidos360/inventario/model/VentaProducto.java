package com.pedidos360.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Unidades vendidas de un producto (ya descontadas las cancelaciones). */
@Entity
@Table(name = "ventas_por_producto")
public class VentaProducto {

    @Id
    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "unidades_vendidas", nullable = false)
    private long unidadesVendidas;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private OffsetDateTime actualizadoEn;

    protected VentaProducto() {}

    public Long getProductoId() { return productoId; }
    public long getUnidadesVendidas() { return unidadesVendidas; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}