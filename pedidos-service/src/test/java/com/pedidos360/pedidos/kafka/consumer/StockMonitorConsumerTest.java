package com.pedidos360.pedidos.kafka.consumer;

import com.pedidos360.pedidos.event.StockActualizadoEvento;
import com.pedidos360.pedidos.service.StockMonitorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Comprueba que el consumidor solo recibe el mensaje y delega en el servicio. */
class StockMonitorConsumerTest {

    private final StockMonitorService servicio = mock(StockMonitorService.class);
    private final StockMonitorConsumer consumidor = new StockMonitorConsumer(servicio);

    @Test
    void entregaElMensajeAlServicioDeStock() {
        ConsumerRecord<String, StockActualizadoEvento> registro = new ConsumerRecord<>("inventario.stock", 0, 3L, "11",
                new StockActualizadoEvento(11L, "Empanada de pino", 3L, 27, Instant.parse("2026-10-21T12:00:00Z")));

        consumidor.alRecibirStockActualizado(registro);

        verify(servicio).registrar(registro);
    }
}