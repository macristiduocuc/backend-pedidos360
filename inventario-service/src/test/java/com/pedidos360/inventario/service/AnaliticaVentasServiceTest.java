package com.pedidos360.inventario.service;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.kafka.consumer.EventoInvalidoException;
import com.pedidos360.inventario.model.VentaRegistrada;
import com.pedidos360.inventario.repository.VentaProductoRepository;
import com.pedidos360.inventario.repository.VentaRegistradaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba la analítica de ventas: suma al crear, resta al cancelar, nunca cuenta dos veces. */
class AnaliticaVentasServiceTest {

    private final VentaProductoRepository ventasPorProducto = mock(VentaProductoRepository.class);
    private final VentaRegistradaRepository ventasRegistradas = mock(VentaRegistradaRepository.class);
    private final AnaliticaVentasService servicio = new AnaliticaVentasService(ventasPorProducto, ventasRegistradas);

    private PedidoEvento evento(PedidoEvento.Tipo tipo, Long pedidoId, List<PedidoEvento.Item> items) {
        return new PedidoEvento(tipo, pedidoId, 3L, "Pendiente", new BigDecimal("1990.00"),
                Instant.parse("2026-10-20T12:00:00Z"), items);
    }

    private List<PedidoEvento.Item> items() {
        return List.of(new PedidoEvento.Item(11L, 2), new PedidoEvento.Item(12L, 1));
    }

    private ConsumerRecord<String, PedidoEvento> registro(PedidoEvento valor) {
        return new ConsumerRecord<>("pedidos.eventos", 0, 10L, "7", valor);
    }

    @Test
    void unPedidoCreadoSumaLasUnidadesDeCadaProductoYQuedaContado() {
        servicio.registrar(registro(evento(PedidoEvento.Tipo.CREADO, 7L, items())));

        verify(ventasPorProducto).sumarUnidades(11L, 2L);
        verify(ventasPorProducto).sumarUnidades(12L, 1L);
        ArgumentCaptor<VentaRegistrada> captor = ArgumentCaptor.forClass(VentaRegistrada.class);
        verify(ventasRegistradas).save(captor.capture());
        assertEquals(7L, captor.getValue().getPedidoId());
    }

    @Test
    void unPedidoCreadoQueLlegaDosVecesNoSeCuentaDosVeces() {
        when(ventasRegistradas.existsById(7L)).thenReturn(true);

        servicio.registrar(registro(evento(PedidoEvento.Tipo.CREADO, 7L, items())));

        verify(ventasPorProducto, never()).sumarUnidades(anyLong(), anyLong());
    }

    @Test
    void unaCancelacionRestaLasUnidadesUnaSolaVez() {
        VentaRegistrada venta = new VentaRegistrada(7L);
        when(ventasRegistradas.findById(7L)).thenReturn(Optional.of(venta));

        servicio.registrar(registro(evento(PedidoEvento.Tipo.CANCELADO, 7L, items())));
        servicio.registrar(registro(evento(PedidoEvento.Tipo.CANCELADO, 7L, items())));

        verify(ventasPorProducto).sumarUnidades(11L, -2L);
        verify(ventasPorProducto).sumarUnidades(12L, -1L);
        assertTrue(venta.isCancelada());
    }

    @Test
    void laCancelacionDeUnPedidoQueNoEstabaContadoSeIgnora() {
        when(ventasRegistradas.findById(7L)).thenReturn(Optional.empty());

        servicio.registrar(registro(evento(PedidoEvento.Tipo.CANCELADO, 7L, items())));

        verify(ventasPorProducto, never()).sumarUnidades(anyLong(), anyLong());
    }

    @Test
    void loscambiosDeEstadoNoAfectanLoVendido() {
        servicio.registrar(registro(evento(PedidoEvento.Tipo.ESTADO_CAMBIADO, 7L, items())));

        verify(ventasPorProducto, never()).sumarUnidades(anyLong(), anyLong());
        verify(ventasRegistradas, never()).save(any(VentaRegistrada.class));
    }

    @Test
    void losEventosIncompletosSeRechazanSinReintentos() {
        assertThrows(EventoInvalidoException.class, () -> servicio.registrar(registro(null)));
        assertThrows(EventoInvalidoException.class,
                () -> servicio.registrar(registro(evento(PedidoEvento.Tipo.CREADO, null, items()))));
        assertThrows(EventoInvalidoException.class, () -> servicio.registrar(
                registro(evento(PedidoEvento.Tipo.CREADO, 7L, List.of(new PedidoEvento.Item(11L, 0))))));
    }

    @Test
    void elLimiteDeLosMasVendidosSeAcotaEntre1Y50() {
        servicio.productosMasVendidos(999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ventasPorProducto).masVendidos(captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }
}