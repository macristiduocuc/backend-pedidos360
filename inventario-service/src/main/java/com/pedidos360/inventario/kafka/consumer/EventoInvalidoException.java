package com.pedidos360.inventario.kafka.consumer;

/** Error IRRECUPERABLE: el evento está incompleto o es ilegible, así que reintentar no sirve. */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}