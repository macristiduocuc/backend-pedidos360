package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.kafka.consumer.EventoInvalidoException;
import com.pedidos360.pedidos.model.AuditoriaKafka;
import com.pedidos360.pedidos.repository.AuditoriaKafkaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba el historial de auditoría: guarda con partición y offset, no duplica y rechaza datos incompletos. */
class AuditoriaKafkaServiceTest {

    private final AuditoriaKafkaRepository repositorio = mock(AuditoriaKafkaRepository.class);
    private final AuditoriaKafkaService servicio = new AuditoriaKafkaService(repositorio);

    private PedidoEvento evento(Long pedidoId, PedidoEvento.Tipo tipo) {
        return new PedidoEvento(tipo, pedidoId, 3L, "Pendiente", new BigDecimal("1990.00"),
                Instant.parse("2026-10-20T12:00:00Z"), List.of(new PedidoEvento.Item(11L, 2)));
    }

    private ConsumerRecord<String, PedidoEvento> registro(PedidoEvento valor) {
        return new ConsumerRecord<>("pedidos.eventos", 1, 42L, "7", valor);
    }

    @Test
    void guardaElEventoConSuParticionYOffset() {
        when(repositorio.existsByTopicoAndParticionAndOffsetRegistro("pedidos.eventos", 1, 42L)).thenReturn(false);

        servicio.registrar(registro(evento(7L, PedidoEvento.Tipo.CREADO)));

        ArgumentCaptor<AuditoriaKafka> captor = ArgumentCaptor.forClass(AuditoriaKafka.class);
        verify(repositorio).save(captor.capture());
        AuditoriaKafka guardado = captor.getValue();
        assertEquals("pedidos.eventos", guardado.getTopico());
        assertEquals(1, guardado.getParticion());
        assertEquals(42L, guardado.getOffsetRegistro());
        assertEquals("7", guardado.getClave());
        assertEquals("CREADO", guardado.getTipo());
        assertEquals(7L, guardado.getPedidoId());
    }

    @Test
    void elMismoMensajeEntregadoDosVecesNoSeDuplica() {
        when(repositorio.existsByTopicoAndParticionAndOffsetRegistro("pedidos.eventos", 1, 42L)).thenReturn(true);

        servicio.registrar(registro(evento(7L, PedidoEvento.Tipo.CREADO)));

        verify(repositorio, never()).save(any(AuditoriaKafka.class));
    }

    @Test
    void unEventoSinIdDePedidoEsInvalido() {
        assertThrows(EventoInvalidoException.class,
                () -> servicio.registrar(registro(evento(null, PedidoEvento.Tipo.CREADO))));

        verify(repositorio, never()).save(any(AuditoriaKafka.class));
    }

    @Test
    void unValorNuloEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> servicio.registrar(registro(null)));
    }
}