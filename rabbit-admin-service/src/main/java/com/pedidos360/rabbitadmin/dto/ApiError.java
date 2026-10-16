package com.pedidos360.rabbitadmin.dto;

import java.time.Instant;

/** Formato único de las respuestas de error de la API. */
public record ApiError(int estado, String error, String mensaje, Instant momento) {
}
