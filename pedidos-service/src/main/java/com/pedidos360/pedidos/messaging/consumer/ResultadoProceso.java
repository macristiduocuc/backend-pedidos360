package com.pedidos360.pedidos.messaging.consumer;

/** Qué debe hacer el consumidor con el mensaje después de intentar procesarlo. */
public enum ResultadoProceso {

    /** Se procesó bien: ack. */
    EXITO,

    /** No se pudo procesar (error irrecuperable o intentos agotados): nack sin reencolar, va a la DLQ. */
    DESCARTADO,

    /** El servicio se está deteniendo: nack con reencolado para no perder el mensaje. */
    REENCOLAR
}