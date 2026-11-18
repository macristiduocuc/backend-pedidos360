package com.pedidos360.pedidos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Un evento del historial de auditoría, con la partición y el offset de Kafka de donde vino. */
public record AuditoriaKafkaResponse(
        Long id,
        String topico,
        int particion,
        long offset,
        String clave,
        String tipo,
        Long pedidoId,
        Long localId,
        String estado,
        BigDecimal total,
        OffsetDateTime ocurridoEn,
        OffsetDateTime recibidoEn) {
}