package com.pedidos360.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Último stock conocido de un producto, recibido desde Kafka. */
@Entity
@Table(name = "stock_actual")
public class StockActual {

    @Id
    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(name = "local_id")
    private Long localId;

    @Column(nullable = false)
    private int stock;

    @Column(name = "ocurrido_en")
    private OffsetDateTime ocurridoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private OffsetDateTime actualizadoEn;

    protected StockActual() {}

    public Long getProductoId() { return productoId; }
    public String getNombreProducto() { return nombreProducto; }
    public Long getLocalId() { return localId; }
    public int getStock() { return stock; }
    public OffsetDateTime getOcurridoEn() { return ocurridoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}