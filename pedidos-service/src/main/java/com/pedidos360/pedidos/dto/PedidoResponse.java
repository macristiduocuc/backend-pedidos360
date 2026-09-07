package com.pedidos360.pedidos.dto;

import com.pedidos360.pedidos.model.Pedido;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String clienteNombre,
        String clienteCorreo,
        Long localId,
        String modalidad,
        String direccionDespacho,
        String estado,
        BigDecimal total,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn,
        List<ItemResponse> items
) {
    public static PedidoResponse desde(Pedido p) {
        return new PedidoResponse(
                p.getId(), p.getClienteNombre(), p.getClienteCorreo(), p.getLocalId(),
                p.getModalidad(), p.getDireccionDespacho(), p.getEstado(), p.getTotal(),
                p.getCreadoEn(), p.getActualizadoEn(),
                p.getItems().stream().map(ItemResponse::desde).toList()
        );
    }
}
