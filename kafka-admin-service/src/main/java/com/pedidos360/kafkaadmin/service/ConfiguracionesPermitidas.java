package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.exception.KafkaAdminException;

import java.util.Map;
import java.util.Set;

/**
 * Configuraciones de tópico que esta API permite crear o modificar, con su validación.
 * Cualquier otra se rechaza, para que nadie deje un tópico con parámetros peligrosos.
 */
final class ConfiguracionesPermitidas {

    static final String CLEANUP_POLICY = "cleanup.policy";
    static final String RETENTION_MS = "retention.ms";
    static final String RETENTION_BYTES = "retention.bytes";
    static final String MIN_INSYNC_REPLICAS = "min.insync.replicas";
    static final String MAX_MESSAGE_BYTES = "max.message.bytes";

    /** Las que se muestran al describir un tópico. */
    static final Set<String> CLAVES = Set.of(
            CLEANUP_POLICY, RETENTION_MS, RETENTION_BYTES, MIN_INSYNC_REPLICAS, MAX_MESSAGE_BYTES);

    private static final Set<String> POLITICAS_VALIDAS = Set.of("delete", "compact", "compact,delete", "delete,compact");

    private ConfiguracionesPermitidas() {
    }

    static void validar(Map<String, String> configuracion, int replicacion) {
        if (configuracion == null) {
            return;
        }
        configuracion.forEach((clave, valor) -> {
            if (!CLAVES.contains(clave)) {
                throw KafkaAdminException.solicitudInvalida("La configuración '" + clave
                        + "' no está permitida. Permitidas: " + String.join(", ", CLAVES.stream().sorted().toList()));
            }
            if (valor == null || valor.isBlank()) {
                throw KafkaAdminException.solicitudInvalida("La configuración '" + clave + "' no puede ir vacía");
            }
            switch (clave) {
                case CLEANUP_POLICY -> {
                    if (!POLITICAS_VALIDAS.contains(valor)) {
                        throw KafkaAdminException.solicitudInvalida(
                                "cleanup.policy debe ser delete, compact o compact,delete");
                    }
                }
                case RETENTION_MS, RETENTION_BYTES -> {
                    long numero = comoNumero(clave, valor);
                    if (numero < -1) {
                        throw KafkaAdminException.solicitudInvalida(clave + " debe ser -1 (sin límite) o mayor");
                    }
                }
                case MIN_INSYNC_REPLICAS -> {
                    long numero = comoNumero(clave, valor);
                    if (numero < 1 || (replicacion > 0 && numero > replicacion)) {
                        throw KafkaAdminException.solicitudInvalida(
                                "min.insync.replicas debe estar entre 1 y el factor de replicación");
                    }
                }
                case MAX_MESSAGE_BYTES -> {
                    if (comoNumero(clave, valor) < 1) {
                        throw KafkaAdminException.solicitudInvalida("max.message.bytes debe ser mayor que 0");
                    }
                }
                default -> {
                }
            }
        });
    }

    private static long comoNumero(String clave, String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            throw KafkaAdminException.solicitudInvalida(clave + " debe ser un número");
        }
    }
}
