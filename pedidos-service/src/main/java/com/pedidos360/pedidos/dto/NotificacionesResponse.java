package com.pedidos360.pedidos.dto;

import java.util.List;

/**
 * Respuesta de la campanita: las últimas notificaciones y cuántas siguen sin leer
 * (el total real, aunque la lista solo traiga las más recientes).
 */
public record NotificacionesResponse(long noLeidas, List<NotificacionResponse> notificaciones) {
}