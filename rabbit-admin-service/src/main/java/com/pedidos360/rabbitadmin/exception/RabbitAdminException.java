package com.pedidos360.rabbitadmin.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio del administrador, con el código HTTP que corresponde a cada caso. */
public class RabbitAdminException extends RuntimeException {

    private final HttpStatus estado;

    private RabbitAdminException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    /** 400: los datos enviados no son válidos. */
    public static RabbitAdminException solicitudInvalida(String mensaje) {
        return new RabbitAdminException(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** 404: el recurso no existe. */
    public static RabbitAdminException noEncontrado(String mensaje) {
        return new RabbitAdminException(HttpStatus.NOT_FOUND, mensaje);
    }

    /** 409: choca con algo que ya existe o está protegido. */
    public static RabbitAdminException conflicto(String mensaje) {
        return new RabbitAdminException(HttpStatus.CONFLICT, mensaje);
    }

    /** 503: no hay conexión con RabbitMQ. */
    public static RabbitAdminException noDisponible(String mensaje) {
        return new RabbitAdminException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }

    /** 502: RabbitMQ respondió con un error inesperado. */
    public static RabbitAdminException errorBroker(String mensaje) {
        return new RabbitAdminException(HttpStatus.BAD_GATEWAY, mensaje);
    }
}
