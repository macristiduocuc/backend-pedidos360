package com.pedidos360.inventario.service;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.inventario.model.ReposicionProcesada;
import com.pedidos360.inventario.repository.ProductoRepository;
import com.pedidos360.inventario.repository.ReposicionProcesadaRepository;
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

/** Comprueba la reposición de stock al cancelar un pedido, incluida la protección contra duplicados. */
class ReposicionStockServiceTest {

    private final ProductoRepository productoRepository = mock(ProductoRepository.class);
    private final ReposicionProcesadaRepository procesadaRepository = mock(ReposicionProcesadaRepository.class);
    private final ReposicionStockService service = new ReposicionStockService(productoRepository, procesadaRepository);

    private PedidoEvento evento(PedidoEvento.Tipo tipo, Long pedidoId, List<PedidoEvento.Item> items) {
        return new PedidoEvento(tipo, pedidoId, 3L, "Cancelado", new BigDecimal("1990.00"), Instant.now(), items);
    }

    private List<PedidoEvento.Item> items() {
        return List.of(new PedidoEvento.Item(11L, 2), new PedidoEvento.Item(12L, 1));
    }

    @Test
    void reponeElStockDeCadaProductoYAnotaElPedido() {
        when(productoRepository.reponerStock(11L, 2)).thenReturn(1);
        when(productoRepository.reponerStock(12L, 1)).thenReturn(1);

        service.reponer(evento(PedidoEvento.Tipo.CANCELADO, 5L, items()));

        verify(productoRepository).reponerStock(11L, 2);
        verify(productoRepository).reponerStock(12L, 1);
        ArgumentCaptor<ReposicionProcesada> captor = ArgumentCaptor.forClass(ReposicionProcesada.class);
        verify(procesadaRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getPedidoId());
    }

    @Test
    void unMensajeDuplicadoNoVuelveAReponerElStock() {
        when(procesadaRepository.existsById(5L)).thenReturn(true);

        service.reponer(evento(PedidoEvento.Tipo.CANCELADO, 5L, items()));

        verify(productoRepository, never()).reponerStock(any(Long.class), any(Integer.class));
        verify(procesadaRepository, never()).save(any(ReposicionProcesada.class));
    }

    @Test
    void siUnProductoNoExisteEsIrrecuperableYNoSeAnotaElPedido() {
        when(productoRepository.reponerStock(11L, 2)).thenReturn(0);

        assertThrows(MensajeNoProcesableException.class,
                () -> service.reponer(evento(PedidoEvento.Tipo.CANCELADO, 5L, items())));

        verify(procesadaRepository, never()).save(any(ReposicionProcesada.class));
    }

    @Test
    void soloSeRepoenePedidosCancelados() {
        assertThrows(MensajeNoProcesableException.class,
                () -> service.reponer(evento(PedidoEvento.Tipo.ESTADO_CAMBIADO, 5L, items())));

        verify(productoRepository, never()).reponerStock(any(Long.class), any(Integer.class));
    }

    @Test
    void unEventoSinIdDePedidoEsIrrecuperable() {
        assertThrows(MensajeNoProcesableException.class,
                () -> service.reponer(evento(PedidoEvento.Tipo.CANCELADO, null, items())));
    }
}