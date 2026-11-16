package com.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Estado de una partición: qué broker es su líder, dónde están sus réplicas y cuáles están al día (ISR). */
public record ParticionInfo(int particion, Integer lider, List<Integer> replicas, List<Integer> sincronizadas) {
}
