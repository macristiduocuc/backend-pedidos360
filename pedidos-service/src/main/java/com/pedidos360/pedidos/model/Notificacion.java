package com.pedidos360.pedidos.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/** Notificación dirigida a un cliente (por correo) o a un rol (por ejemplo AUDITORIA). */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    private static final int LARGO_MAXIMO_MENSAJE = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "destinatario_correo", length = 150)
    private String destinatarioCorreo;

    @Column(name = "destinatario_rol", length = 30)
    private String destinatarioRol;

    @Column(nullable = false, length = LARGO_MAXIMO_MENSAJE)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    public Notificacion() {}

    public static Notificacion paraCliente(Long pedidoId, String correo, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.pedidoId = pedidoId;
        notificacion.destinatarioCorreo = correo;
        notificacion.mensaje = recortar(mensaje);
        return notificacion;
    }

    public static Notificacion paraRol(Long pedidoId, String rol, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.pedidoId = pedidoId;
        notificacion.destinatarioRol = rol;
        notificacion.mensaje = recortar(mensaje);
        return notificacion;
    }

    private static String recortar(String texto) {
        return texto.length() > LARGO_MAXIMO_MENSAJE ? texto.substring(0, LARGO_MAXIMO_MENSAJE) : texto;
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public String getDestinatarioCorreo() { return destinatarioCorreo; }
    public String getDestinatarioRol() { return destinatarioRol; }
    public String getMensaje() { return mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}