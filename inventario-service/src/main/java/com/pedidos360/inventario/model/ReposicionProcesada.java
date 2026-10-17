package com.pedidos360.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Marca de que el stock de un pedido cancelado ya fue repuesto (protección contra mensajes duplicados). */
@Entity
@Table(name = "reposiciones_procesadas")
public class ReposicionProcesada {

    @Id
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "procesado_en", insertable = false, updatable = false)
    private OffsetDateTime procesadoEn;

    protected ReposicionProcesada() {}

    public ReposicionProcesada(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Long getPedidoId() { return pedidoId; }
    public OffsetDateTime getProcesadoEn() { return procesadoEn; }
}