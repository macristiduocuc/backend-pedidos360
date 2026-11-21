package com.pedidos360.kafkaadmin.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio del administrador, con el código HTTP que corresponde a cada caso. */
public class KafkaAdminException extends RuntimeException {

    private final HttpStatus estado;

    private KafkaAdminException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    /** 400: los datos enviados no son válidos. */
    public static KafkaAdminException solicitudInvalida(String mensaje) {
        return new KafkaAdminException(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** 404: el recurso no existe. */
    public static KafkaAdminException noEncontrado(String mensaje) {
        return new KafkaAdminException(HttpStatus.NOT_FOUND, mensaje);
    }

    /** 409: choca con algo que ya existe o está protegido. */
    public static KafkaAdminException conflicto(String mensaje) {
        return new KafkaAdminException(HttpStatus.CONFLICT, mensaje);
    }

    /** 503: no hay conexión con los brokers de Kafka. */
    public static KafkaAdminException noDisponible(String mensaje) {
        return new KafkaAdminException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }

    /** 502: Kafka respondió con un error inesperado. */
    public static KafkaAdminException errorBroker(String mensaje) {
        return new KafkaAdminException(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
