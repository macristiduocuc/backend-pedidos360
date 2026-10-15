package com.pedidos360.pedidos.messaging.consumer;

/**
 * Error IRRECUPERABLE: reintentar no sirve (mensaje mal formado, pedido que no existe, datos
 * incompletos). El mensaje se rechaza sin reencolar y viaja a la DLQ.
 */
public class MensajeNoProcesableException extends RuntimeException {

    public MensajeNoProcesableException(String mensaje) {
        super(mensaje);
    }

    public MensajeNoProcesableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}