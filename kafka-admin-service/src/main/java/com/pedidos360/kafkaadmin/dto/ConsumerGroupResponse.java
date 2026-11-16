package com.pedidos360.kafkaadmin.dto;

import java.util.List;

/**
 * Estado de un consumer group.
 *
 * @param lagTotal mensajes que el grupo todavía no ha leído (suma de todas sus particiones)
 */
public record ConsumerGroupResponse(String id, String estado, int miembros, List<MiembroInfo> detalle, long lagTotal) {
}
