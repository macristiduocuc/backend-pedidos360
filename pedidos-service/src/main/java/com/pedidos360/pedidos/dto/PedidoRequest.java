package com.pedidos360.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record PedidoRequest(
        @NotBlank String clienteNombre,
        @NotBlank String clienteCorreo,
        @NotNull Long localId,
        @Pattern(regexp = "Retiro en tienda|Despacho a domicilio", message = "modalidad inválida")
        String modalidad,
        String direccionDespacho,
        @NotEmpty List<@Valid ItemRequest> items
) {}
