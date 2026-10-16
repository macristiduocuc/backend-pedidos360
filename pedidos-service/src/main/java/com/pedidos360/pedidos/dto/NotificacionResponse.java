package com.pedidos360.pedidos.dto;

import java.time.OffsetDateTime;

/** Una notificación tal como la ve el usuario en la campanita. */
public record NotificacionResponse(Long id, Long pedidoId, String mensaje, boolean leida, OffsetDateTime creadoEn) {
}