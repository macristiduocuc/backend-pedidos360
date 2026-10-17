package com.pedidos360.inventario.messaging.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * Convertidor "que no convierte": entrega el cuerpo del mensaje tal cual (bytes).
 *
 * Los consumidores lo usan para que Spring NO intente interpretar el JSON antes de tiempo.
 * La conversión la hace ProcesadorMensajes, que además decide qué hacer si falla
 * (nack hacia la DLQ y registro en el log). Así ningún mensaje queda sin confirmar.
 */
public class ConvertidorCrudo implements MessageConverter {

    @Override
    public Message toMessage(Object objeto, MessageProperties propiedades) throws MessageConversionException {
        throw new MessageConversionException("Este convertidor solo sirve para recibir mensajes, no para enviarlos");
    }

    @Override
    public Object fromMessage(Message mensaje) throws MessageConversionException {
        return mensaje.getBody();
    }
}