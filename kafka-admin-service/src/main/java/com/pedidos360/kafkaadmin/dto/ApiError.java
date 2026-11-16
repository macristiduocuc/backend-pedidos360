package com.pedidos360.kafkaadmin.dto;

import java.time.Instant;

/** Formato único de las respuestas de error de la API. */
public record ApiError(int estado, String error, String mensaje, Instant momento) {
}
