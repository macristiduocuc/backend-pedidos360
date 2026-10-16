package com.pedidos360.inventario.messaging.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConvertidorCrudoTest {

    private final ConvertidorCrudo convertidor = new ConvertidorCrudo();

    @Test
    void entregaElCuerpoSinConvertirNadaAunqueNoSeaJsonValido() {
        byte[] cuerpo = "esto no es json".getBytes(StandardCharsets.UTF_8);
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType("application/json");

        Object resultado = convertidor.fromMessage(new Message(cuerpo, propiedades));

        assertArrayEquals(cuerpo, (byte[]) resultado);
    }

    @Test
    void noSirveParaEnviarMensajes() {
        assertThrows(MessageConversionException.class,
                () -> convertidor.toMessage("hola", new MessageProperties()));
    }
}