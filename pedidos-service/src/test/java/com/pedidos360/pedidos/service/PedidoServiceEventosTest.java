package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.client.InventarioClient;
import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.model.PedidoItem;
import com.pedidos360.pedidos.repository.EventoAuditoriaRepository;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba que PedidoService anuncia los eventos correctos sin conocer RabbitMQ. */
class PedidoServiceEventosTest {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final EventoAuditoriaRepository eventoRepository = mock(EventoAuditoriaRepository.class);
    private final InventarioClient inventarioClient = mock(InventarioClient.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final PedidoService service =
            new PedidoService(pedidoRepository, eventoRepository, inventarioClient, eventPublisher);

    private Pedido pedidoGuardado(String estado) {
        PedidoItem item = new PedidoItem();
        item.setProductoId(11L);
        item.setCantidad(2);

        Pedido pedido = new Pedido();
        pedido.setId(5L);
        pedido.setLocalId(3L);
        pedido.setEstado(estado);
        pedido.setTotal(new BigDecimal("1990.00"));
        pedido.setItems(List.of(item));

        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        return pedido;
    }

    private PedidoEvento eventoPublicado() {
        ArgumentCaptor<PedidoEvento> captor = ArgumentCaptor.forClass(PedidoEvento.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void cambiarEstadoCocinaAnunciaUnEventoDeEstado() {
        pedidoGuardado("Pendiente");

        service.cambiarEstadoCocina(5L, "En preparación");

        PedidoEvento evento = eventoPublicado();
        assertEquals(PedidoEvento.Tipo.ESTADO_CAMBIADO, evento.tipo());
        assertEquals(5L, evento.pedidoId());
        assertEquals(3L, evento.localId());
        assertEquals("En preparación", evento.estado());
    }

    @Test
    void marcarEntregadoAnunciaUnEventoDeEstado() {
        pedidoGuardado("Hecho");

        service.marcarEntregado(5L);

        PedidoEvento evento = eventoPublicado();
        assertEquals(PedidoEvento.Tipo.ESTADO_CAMBIADO, evento.tipo());
        assertEquals("Entregado", evento.estado());
    }

    @Test
    void cancelarPedidoAnunciaUnEventoConLosItemsParaReponerStock() {
        pedidoGuardado("Pendiente");

        service.cancelarPedido(5L);

        PedidoEvento evento = eventoPublicado();
        assertEquals(PedidoEvento.Tipo.CANCELADO, evento.tipo());
        assertEquals("Cancelado", evento.estado());
        assertEquals(List.of(new PedidoEvento.Item(11L, 2)), evento.items());
    }

    @Test
    void siElPedidoYaFueEntregadoNoSeCancelaNiSeAnunciaNada() {
        pedidoGuardado("Entregado");

        assertThrows(ResponseStatusException.class, () -> service.cancelarPedido(5L));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}