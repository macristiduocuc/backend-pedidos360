package com.pedidos360.pedidos.messaging.consumer;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.config.ConsumidorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Comprueba la política de errores: ack, descarte a la DLQ y reintentos. */
class ProcesadorMensajesTest {

    private final MessageConverter converter = new JacksonJsonMessageConverter();
    private final ProcesadorMensajes procesador =
            new ProcesadorMensajes(converter, new ConsumidorProperties(3, Duration.ZERO));

    private Message mensajeValido() {
        PedidoEvento evento = new PedidoEvento(PedidoEvento.Tipo.CREADO, 5L, 3L, "Pendiente",
                new BigDecimal("1990.00"), Instant.parse("2026-10-14T12:00:00Z"),
                List.of(new PedidoEvento.Item(11L, 2)));
        return converter.toMessage(evento, new MessageProperties());
    }

    @Test
    void unMensajeValidoSeProcesaYSeConfirma() {
        AtomicReference<PedidoEvento> recibido = new AtomicReference<>();

        ResultadoProceso resultado = procesador.procesar(mensajeValido(), PedidoEvento.class, recibido::set);

        assertEquals(ResultadoProceso.EXITO, resultado);
        assertNotNull(recibido.get());
        assertEquals(5L, recibido.get().pedidoId());
        assertEquals(3L, recibido.get().localId());
        assertEquals(PedidoEvento.Tipo.CREADO, recibido.get().tipo());
        assertEquals(0, new BigDecimal("1990.00").compareTo(recibido.get().total()));
        assertEquals(List.of(new PedidoEvento.Item(11L, 2)), recibido.get().items());
    }

    @Test
    void unContenidoQueNoEsJsonSeDescartaSinReintentos() {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType("application/json");
        Message basura = new Message("esto no es json".getBytes(StandardCharsets.UTF_8), propiedades);
        AtomicInteger invocaciones = new AtomicInteger();

        ResultadoProceso resultado = procesador.procesar(basura, PedidoEvento.class, e -> invocaciones.incrementAndGet());

        assertEquals(ResultadoProceso.DESCARTADO, resultado);
        assertEquals(0, invocaciones.get());
    }

    @Test
    void unErrorIrrecuperableSeDescartaSinReintentar() {
        AtomicInteger invocaciones = new AtomicInteger();

        ResultadoProceso resultado = procesador.procesar(mensajeValido(), PedidoEvento.class, e -> {
            invocaciones.incrementAndGet();
            throw new MensajeNoProcesableException("El pedido 5 no existe");
        });

        assertEquals(ResultadoProceso.DESCARTADO, resultado);
        assertEquals(1, invocaciones.get());
    }

    @Test
    void unErrorRecuperableSeReintentaYLuegoVaALaDlq() {
        AtomicInteger invocaciones = new AtomicInteger();

        ResultadoProceso resultado = procesador.procesar(mensajeValido(), PedidoEvento.class, e -> {
            invocaciones.incrementAndGet();
            throw new IllegalStateException("La base de datos no responde");
        });

        assertEquals(ResultadoProceso.DESCARTADO, resultado);
        assertEquals(3, invocaciones.get());
    }

    @Test
    void unErrorRecuperableQueSeResuelveTerminaEnExito() {
        AtomicInteger invocaciones = new AtomicInteger();

        ResultadoProceso resultado = procesador.procesar(mensajeValido(), PedidoEvento.class, e -> {
            if (invocaciones.incrementAndGet() < 3) {
                throw new IllegalStateException("Falla momentánea");
            }
        });

        assertEquals(ResultadoProceso.EXITO, resultado);
        assertEquals(3, invocaciones.get());
    }
}