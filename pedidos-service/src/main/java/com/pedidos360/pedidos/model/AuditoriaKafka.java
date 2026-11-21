package com.pedidos360.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Un evento de pedido recibido desde Kafka, con la partición y el offset de donde vino. */
@Entity
@Table(name = "auditoria_kafka")
public class AuditoriaKafka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 249)
    private String topico;

    @Column(nullable = false)
    private int particion;

    @Column(name = "offset_registro", nullable = false)
    private long offsetRegistro;

    @Column(length = 100)
    private String clave;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "local_id")
    private Long localId;

    @Column(length = 30)
    private String estado;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "ocurrido_en")
    private OffsetDateTime ocurridoEn;

    @Column(name = "recibido_en", insertable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    protected AuditoriaKafka() {}

    public AuditoriaKafka(String topico, int particion, long offsetRegistro, String clave, String tipo,
                          Long pedidoId, Long localId, String estado, BigDecimal total, OffsetDateTime ocurridoEn) {
        this.topico = topico;
        this.particion = particion;
        this.offsetRegistro = offsetRegistro;
        this.clave = clave;
        this.tipo = tipo;
        this.pedidoId = pedidoId;
        this.localId = localId;
        this.estado = estado;
        this.total = total;
        this.ocurridoEn = ocurridoEn;
    }

    public Long getId() { return id; }
    public String getTopico() { return topico; }
    public int getParticion() { return particion; }
    public long getOffsetRegistro() { return offsetRegistro; }
    public String getClave() { return clave; }
    public String getTipo() { return tipo; }
    public Long getPedidoId() { return pedidoId; }
    public Long getLocalId() { return localId; }
    public String getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
    public OffsetDateTime getOcurridoEn() { return ocurridoEn; }
    public OffsetDateTime getRecibidoEn() { return recibidoEn; }
}