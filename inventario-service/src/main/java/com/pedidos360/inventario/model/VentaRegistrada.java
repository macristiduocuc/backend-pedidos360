package com.pedidos360.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Pedido ya contado en la analítica de ventas (protege contra mensajes duplicados). */
@Entity
@Table(name = "ventas_registradas")
public class VentaRegistrada {

    @Id
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(nullable = false)
    private boolean cancelada;

    @Column(name = "registrada_en", insertable = false, updatable = false)
    private OffsetDateTime registradaEn;

    protected VentaRegistrada() {}

    public VentaRegistrada(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public void marcarCancelada() {
        this.cancelada = true;
    }

    public Long getPedidoId() { return pedidoId; }
    public boolean isCancelada() { return cancelada; }
    public OffsetDateTime getRegistradaEn() { return registradaEn; }
}