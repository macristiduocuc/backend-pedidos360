package com.pedidos360.inventario.service;

import com.pedidos360.inventario.config.InventarioProperties;
import com.pedidos360.inventario.event.AlertaStockEvento;
import com.pedidos360.inventario.event.StockActualizadoEvento;
import com.pedidos360.inventario.model.Local;
import com.pedidos360.inventario.model.Producto;
import com.pedidos360.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Comprueba cuándo una reserva de stock anuncia una alerta de stock bajo (umbral = 10) y
 * que cada cambio de stock anuncia el stock actual para Kafka.
 */
class StockServiceTest {

    private final ProductoRepository productoRepository = mock(ProductoRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final StockService service = new StockService(productoRepository, eventPublisher, new InventarioProperties(10));

    private Producto producto() {
        Local local = new Local();
        local.setId(3L);
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Empanada de pino");
        producto.setLocal(local);
        return producto;
    }

    private void productoConStockAntes(int stockAntes, int cantidad) {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto()));
        when(productoRepository.reservarStock(1L, cantidad)).thenReturn(1);
        when(productoRepository.stockActual(1L)).thenReturn(stockAntes - cantidad);
    }

    private List<Object> publicados() {
        return mockingDetails(eventPublisher).getInvocations().stream()
                .filter(invocacion -> invocacion.getMethod().getName().equals("publishEvent"))
                .map(invocacion -> (Object) invocacion.getArgument(0))
                .toList();
    }

    private List<AlertaStockEvento> alertas() {
        return publicados().stream().filter(AlertaStockEvento.class::isInstance).map(AlertaStockEvento.class::cast).toList();
    }

    private List<StockActualizadoEvento> stocks() {
        return publicados().stream().filter(StockActualizadoEvento.class::isInstance).map(StockActualizadoEvento.class::cast).toList();
    }

    // ---- reservas (compras) ----

    @Test
    void siLaReservaCruzaElUmbralAnunciaLaAlerta() {
        productoConStockAntes(12, 3);

        service.reservar(1L, 3);

        assertEquals(1, alertas().size());
        AlertaStockEvento alerta = alertas().get(0);
        assertEquals(1L, alerta.productoId());
        assertEquals("Empanada de pino", alerta.nombreProducto());
        assertEquals(3L, alerta.localId());
        assertEquals(9, alerta.stockActual());
        assertEquals(10, alerta.stockMinimo());
    }

    @Test
    void siSobraMuchoStockNoAnunciaAlerta() {
        productoConStockAntes(30, 3);

        service.reservar(1L, 3);

        assertTrue(alertas().isEmpty());
    }

    @Test
    void siElProductoYaEstabaBajoNoVuelveAAvisarEnCadaCompra() {
        productoConStockAntes(8, 2);

        service.reservar(1L, 2);

        assertTrue(alertas().isEmpty());
    }

    @Test
    void siSeAgotaAvisaAunqueYaEstuvieraBajo() {
        productoConStockAntes(4, 4);

        service.reservar(1L, 4);

        assertEquals(1, alertas().size());
        assertEquals(0, alertas().get(0).stockActual());
    }

    @Test
    void cadaReservaAnunciaElStockActualParaKafka() {
        productoConStockAntes(30, 3);

        service.reservar(1L, 3);

        assertEquals(1, stocks().size());
        StockActualizadoEvento stock = stocks().get(0);
        assertEquals(1L, stock.productoId());
        assertEquals("Empanada de pino", stock.nombreProducto());
        assertEquals(3L, stock.localId());
        assertEquals(27, stock.stock());
    }

    @Test
    void siNoAlcanzaElStockResponde409YNoAnunciaNada() {
        productoConStockAntes(2, 5);
        when(productoRepository.reservarStock(1L, 5)).thenReturn(0);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.reservar(1L, 5));

        assertEquals(409, error.getStatusCode().value());
        assertTrue(publicados().isEmpty());
    }

    @Test
    void siElProductoNoExisteResponde404() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.reservar(99L, 1));

        assertEquals(404, error.getStatusCode().value());
    }

    // ---- cambios hechos por Auditoría en la gestión de inventario ----

    @Test
    void siAuditoriaBajaElStockPorDebajoDelUmbralSeAnunciaLaAlerta() {
        service.evaluarAlerta(producto(), 15, 9);

        assertEquals(1, alertas().size());
        assertEquals(9, alertas().get(0).stockActual());
        assertEquals(1L, alertas().get(0).productoId());
    }

    @Test
    void siSoloSeEditaOtroDatoYElStockNoCambiaNoSeAnunciaNada() {
        service.evaluarAlerta(producto(), 5, 5);

        assertTrue(alertas().isEmpty());
    }

    @Test
    void siAuditoriaSubeElStockNoSeAnunciaAlerta() {
        service.evaluarAlerta(producto(), 4, 20);

        assertTrue(alertas().isEmpty());
    }

    @Test
    void unProductoNuevoConPocoStockAnunciaLaAlerta() {
        service.evaluarAlerta(producto(), null, 6);

        assertEquals(1, alertas().size());
    }

    @Test
    void unProductoNuevoConMuchoStockNoAnunciaAlerta() {
        service.evaluarAlerta(producto(), null, 50);

        assertTrue(alertas().isEmpty());
    }

    // ---- anuncio del stock actual ----

    @Test
    void anunciarStockPublicaElEventoConLosDatosDelProducto() {
        service.anunciarStock(producto(), 18);

        assertEquals(1, stocks().size());
        assertEquals(18, stocks().get(0).stock());
        assertEquals("Empanada de pino", stocks().get(0).nombreProducto());
    }
}