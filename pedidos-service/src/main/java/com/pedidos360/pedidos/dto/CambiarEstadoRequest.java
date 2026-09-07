package com.pedidos360.pedidos.dto;

import jakarta.validation.constraints.Pattern;

public record CambiarEstadoRequest(
        @Pattern(regexp = "En preparación|Hecho", message = "estado inválido para cocina")
        String nuevoEstado
) {}
