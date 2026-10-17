package com.pedidos360.pedidos.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento de dominio que PedidoService anuncia cuando un pedido se crea, cambia de estado
 * o se cancela. Es también el contenido (JSON) del mensaje que viaja por RabbitMQ.
 *
 * IMPORTANTE: no lleva datos personales (ni nombre, ni correo, ni dirección del cliente).
 * Solo ids, estado, total y hora. Quien necesite datos del cliente los busca en la base
 * de datos, dentro del backend, usando el pedidoId.
 */
public record PedidoEvento(
        Tipo tipo,
        Long pedidoId,
        Long localId,
        String estado,
        BigDecimal total,
        Instant ocurridoEn,
        List<Item> items) {

    public enum Tipo {
        CREADO,
        ESTADO_CAMBIADO,
        CANCELADO
    }

    /** Producto y cantidad del pedido (los necesita inventario para reponer stock al cancelar). */
    public record Item(Long productoId, Integer cantidad) {
    }
}