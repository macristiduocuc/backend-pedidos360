package com.pedidos360.pedidos.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String rol;

    @Column(name = "fecha", insertable = false, updatable = false)
    private OffsetDateTime fecha;

    public EventoAuditoria() {}

    public EventoAuditoria(Long pedidoId, String descripcion, String rol) {
        this.pedidoId = pedidoId;
        this.descripcion = descripcion;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public String getDescripcion() { return descripcion; }
    public String getRol() { return rol; }
    public OffsetDateTime getFecha() { return fecha; }
}
