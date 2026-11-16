package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * Datos para crear un tópico.
 *
 * @param nombre        letras, números, punto, guion y guion bajo (máximo 249 caracteres)
 * @param particiones   entre 1 y 50: más particiones permiten más consumidores en paralelo
 * @param replicacion   entre 1 y 3: en cuántos brokers se copia cada partición (no puede superar los brokers que existan)
 * @param configuracion opcional: cleanup.policy, retention.ms, retention.bytes, min.insync.replicas o max.message.bytes
 */
public record TopicRequest(
        @NotBlank(message = "El nombre del tópico es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9._-]{1,249}$", message = "El nombre solo puede tener letras, números, punto, guion y guion bajo (máximo 249 caracteres)")
        String nombre,
        @NotNull(message = "Las particiones son obligatorias")
        @Min(value = 1, message = "Debe haber al menos 1 partición")
        @Max(value = 50, message = "No se permiten más de 50 particiones")
        Integer particiones,
        @NotNull(message = "El factor de replicación es obligatorio")
        @Min(value = 1, message = "La replicación mínima es 1")
        @Max(value = 3, message = "La replicación máxima es 3")
        Integer replicacion,
        Map<String, String> configuracion) {
}
