package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Configuraciones que se quieren cambiar en un tópico existente. */
public record TopicConfigRequest(
        @NotEmpty(message = "Indica al menos una configuración para cambiar")
        Map<String, String> configuracion) {
}
