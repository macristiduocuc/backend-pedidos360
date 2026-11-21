package com.pedidos360.inventario.kafka.consumer;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.service.AnaliticaVentasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Comprueba que el consumidor solo recibe el mensaje y delega en el servicio. */
class AnaliticaVentasConsumerTest {

    private final AnaliticaVentasService servicio = mock(AnaliticaVentasService.class);
    private final AnaliticaVentasConsumer consumidor = new AnaliticaVentasConsumer(servicio);

    @Test
    void entregaElMensajeAlServicioDeAnalitica() {
        PedidoEvento evento = new PedidoEvento(PedidoEvento.Tipo.CREADO, 7L, 3L, "Pendiente",
                new BigDecimal("1990.00"), Instant.parse("2026-10-20T12:00:00Z"), List.of());
        ConsumerRecord<String, PedidoEvento> registro = new ConsumerRecord<>("pedidos.eventos", 0, 5L, "7", evento);

        consumidor.alRecibirEventoDePedido(registro);

        verify(servicio).registrar(registro);
    }
}