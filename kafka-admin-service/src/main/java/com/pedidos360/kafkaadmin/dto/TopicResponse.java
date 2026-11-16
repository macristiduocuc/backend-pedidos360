package com.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record TopicResponse(
        String nombre,
        int particiones,
        int replicacion,
        Map<String, String> configuracion,
        List<ParticionInfo> detalle) {
}
