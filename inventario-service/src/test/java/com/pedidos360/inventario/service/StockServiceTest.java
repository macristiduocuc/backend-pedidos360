package com.pedidos360.inventario.service;

import com.pedidos360.inventario.config.InventarioProperties;
import com.pedidos360.inventario.event.AlertaStockEvento;
import com.pedidos360.inventario.model.Local;
import com.pedidos360.inventario.model.Producto;
import com.pedidos360.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba cuándo la reserva de stock anuncia una alerta de stock bajo (umbral = 10). */
class StockServiceTest {

    private final ProductoRepository productoRepository = mock(ProductoRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final StockService service = new StockService(productoRepository, eventPublisher, new InventarioProperties(10));

    private void productoConStockAntes(int stockAntes, int cantidad) {
        Local local = new Local();
        local.setId(3L);
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Empanada de pino");
        producto.setLocal(local);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.reservarStock(1L, cantidad)).thenReturn(1);
        when(productoRepository.stockActual(1L)).thenReturn(stockAntes - cantidad);
    }

    @Test
    void siLaReservaCruzaElUmbralAnunciaLaAlerta() {
        productoConStockAntes(12, 3);

        service.reservar(1L, 3);

        ArgumentCaptor<AlertaStockEvento> captor = ArgumentCaptor.forClass(AlertaStockEvento.class);
        verify(eventPublisher).publishEvent(captor.capture());
        AlertaStockEvento alerta = captor.getValue();
        assertEquals(1L, alerta.productoId());
        assertEquals("Empanada de pino", alerta.nombreProducto());
        assertEquals(3L, alerta.localId());
        assertEquals(9, alerta.stockActual());
        assertEquals(10, alerta.stockMinimo());
    }

    @Test
    void siSobraMuchoStockNoAnunciaNada() {
        productoConStockAntes(30, 3);

        service.reservar(1L, 3);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void siElProductoYaEstabaBajoNoVuelveAAvisarEnCadaCompra() {
        productoConStockAntes(8, 2);

        service.reservar(1L, 2);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void siSeAgotaAvisaAunqueYaEstuvieraBajo() {
        productoConStockAntes(4, 4);

        service.reservar(1L, 4);

        ArgumentCaptor<AlertaStockEvento> captor = ArgumentCaptor.forClass(AlertaStockEvento.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(0, captor.getValue().stockActual());
    }

    @Test
    void siNoAlcanzaElStockResponde409YNoAnunciaNada() {
        productoConStockAntes(2, 5);
        when(productoRepository.reservarStock(1L, 5)).thenReturn(0);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.reservar(1L, 5));

        assertEquals(409, error.getStatusCode().value());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void siElProductoNoExisteResponde404() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.reservar(99L, 1));

        assertEquals(404, error.getStatusCode().value());
    }
}