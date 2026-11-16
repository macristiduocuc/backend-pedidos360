package com.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Un consumidor del grupo y las particiones que tiene asignadas. */
public record MiembroInfo(String clientId, String host, List<String> particiones) {
}
