package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.StockActualizadoEvento;
import com.pedidos360.pedidos.kafka.consumer.EventoInvalidoException;
import com.pedidos360.pedidos.repository.StockActualRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Comprueba que el monitor guarda el último stock de cada producto y rechaza mensajes incompletos. */
class StockMonitorServiceTest {

    private final StockActualRepository repositorio = mock(StockActualRepository.class);
    private final StockMonitorService servicio = new StockMonitorService(repositorio);

    private ConsumerRecord<String, StockActualizadoEvento> registro(StockActualizadoEvento valor) {
        return new ConsumerRecord<>("inventario.stock", 1, 7L, "11", valor);
    }

    @Test
    void guardaElStockActualDelProducto() {
        Instant momento = Instant.parse("2026-10-21T12:00:00Z");

        servicio.registrar(registro(new StockActualizadoEvento(11L, "Empanada de pino", 3L, 27, momento)));

        verify(repositorio).guardar(11L, "Empanada de pino", 3L, 27, OffsetDateTime.ofInstant(momento, ZoneOffset.UTC));
    }

    @Test
    void siElMensajeNoTraeFechaSeUsaLaHoraActual() {
        servicio.registrar(registro(new StockActualizadoEvento(11L, "Empanada de pino", 3L, 27, null)));

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repositorio).guardar(anyLong(), anyString(), anyLong(), anyInt(), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void unMensajeSinProductoOSinStockEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> servicio.registrar(registro(null)));
        assertThrows(EventoInvalidoException.class,
                () -> servicio.registrar(registro(new StockActualizadoEvento(null, "x", 3L, 5, null))));
        assertThrows(EventoInvalidoException.class,
                () -> servicio.registrar(registro(new StockActualizadoEvento(11L, "x", 3L, null, null))));

        verify(repositorio, never()).guardar(anyLong(), anyString(), anyLong(), anyInt(), any(OffsetDateTime.class));
    }

    @Test
    void unStockNegativoEsInvalido() {
        assertThrows(EventoInvalidoException.class,
                () -> servicio.registrar(registro(new StockActualizadoEvento(11L, "x", 3L, -1, null))));
    }
}