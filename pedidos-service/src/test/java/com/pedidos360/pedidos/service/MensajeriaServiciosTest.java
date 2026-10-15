package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.AlertaStockEvento;
import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.pedidos.model.EventoAuditoria;
import com.pedidos360.pedidos.model.Notificacion;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.model.PedidoItem;
import com.pedidos360.pedidos.repository.EventoAuditoriaRepository;
import com.pedidos360.pedidos.repository.NotificacionRepository;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba lo que hacen los consumidores con cada mensaje (la lógica de negocio de la mensajería). */
class MensajeriaServiciosTest {

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final EventoAuditoriaRepository eventoRepository = mock(EventoAuditoriaRepository.class);
    private final NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);

    private final ComandaService comandaService = new ComandaService(pedidoRepository, eventoRepository);
    private final NotificacionService notificacionService = new NotificacionService(pedidoRepository, notificacionRepository);
    private final AlertaStockService alertaStockService = new AlertaStockService(notificacionRepository);

    private PedidoItem item(Long productoId, String nombre, int cantidad) {
        PedidoItem item = new PedidoItem();
        item.setProductoId(productoId);
        item.setNombreProducto(nombre);
        item.setCantidad(cantidad);
        return item;
    }

    private void existeElPedido5() {
        Pedido pedido = new Pedido();
        pedido.setId(5L);
        pedido.setLocalId(3L);
        pedido.setModalidad("Retiro en tienda");
        pedido.setClienteNombre("Ana Pérez");
        pedido.setClienteCorreo("ana@correo.cl");
        pedido.setItems(List.of(item(11L, "Empanada de pino", 2), item(12L, "Bebida", 1)));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
    }

    private PedidoEvento evento(PedidoEvento.Tipo tipo, String estado) {
        return new PedidoEvento(tipo, 5L, 3L, estado, new BigDecimal("1990.00"), Instant.now(), List.of());
    }

    @Test
    void laComandaDescribeLosProductosYQuedaRegistradaParaCocina() {
        existeElPedido5();

        comandaService.registrarComanda(evento(PedidoEvento.Tipo.CREADO, "Pendiente"));

        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(eventoRepository).save(captor.capture());
        EventoAuditoria guardado = captor.getValue();
        assertEquals("cocina", guardado.getRol());
        assertEquals(5L, guardado.getPedidoId());
        assertTrue(guardado.getDescripcion().contains("#5"));
        assertTrue(guardado.getDescripcion().contains("2 x Empanada de pino"));
        assertTrue(guardado.getDescripcion().contains("1 x Bebida"));
    }

    @Test
    void laComandaDeUnPedidoInexistenteEsUnErrorIrrecuperable() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(MensajeNoProcesableException.class,
                () -> comandaService.registrarComanda(evento(PedidoEvento.Tipo.CREADO, "Pendiente")));
    }

    @Test
    void laComandaSoloSeGeneraParaPedidosCreados() {
        existeElPedido5();

        assertThrows(MensajeNoProcesableException.class,
                () -> comandaService.registrarComanda(evento(PedidoEvento.Tipo.CANCELADO, "Cancelado")));
    }

    @Test
    void laNotificacionUsaElCorreoDelPedidoGuardadoEnLaBaseDeDatos() {
        existeElPedido5();

        notificacionService.notificarCliente(evento(PedidoEvento.Tipo.ESTADO_CAMBIADO, "Entregado"));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals("ana@correo.cl", guardada.getDestinatarioCorreo());
        assertNull(guardada.getDestinatarioRol());
        assertEquals(5L, guardada.getPedidoId());
        assertTrue(guardada.getMensaje().contains("#5"));
        assertTrue(guardada.getMensaje().contains("Entregado"));
    }

    @Test
    void laNotificacionDeUnPedidoInexistenteEsUnErrorIrrecuperable() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(MensajeNoProcesableException.class,
                () -> notificacionService.notificarCliente(evento(PedidoEvento.Tipo.CREADO, "Pendiente")));
    }

    @Test
    void laAlertaDeStockBajoVaDirigidaAlRolAuditoria() {
        alertaStockService.registrarAlerta(
                new AlertaStockEvento(1L, "Empanada de pino", 3L, 2, 5, Instant.now()));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals("AUDITORIA", guardada.getDestinatarioRol());
        assertNull(guardada.getDestinatarioCorreo());
        assertTrue(guardada.getMensaje().contains("Empanada de pino"));
        assertTrue(guardada.getMensaje().contains("quedan 2"));
    }

    @Test
    void unaAlertaSinProductoEsUnErrorIrrecuperable() {
        assertThrows(MensajeNoProcesableException.class,
                () -> alertaStockService.registrarAlerta(new AlertaStockEvento(null, null, 3L, 2, 5, Instant.now())));
    }
}