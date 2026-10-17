package com.pedidos360.inventario.service;

import com.pedidos360.inventario.config.InventarioProperties;
import com.pedidos360.inventario.event.AlertaStockEvento;
import com.pedidos360.inventario.model.Producto;
import com.pedidos360.inventario.repository.ProductoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Reserva de stock y alertas de stock bajo. Cuando un cambio de stock deja el producto con poco
 * stock, anuncia una alerta (sin conocer RabbitMQ: quien la publica es AlertaStockPublisher, tras
 * el commit). El cambio puede venir de una compra (reservar) o de que Auditoría edite el producto
 * en la gestión de inventario (evaluarAlerta).
 */
@Service
public class StockService {

    private final ProductoRepository productoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventarioProperties props;

    public StockService(ProductoRepository productoRepository,
                        ApplicationEventPublisher eventPublisher,
                        InventarioProperties props) {
        this.productoRepository = productoRepository;
        this.eventPublisher = eventPublisher;
        this.props = props;
    }

    /** 404 si el producto no existe; 409 si no alcanza el stock (el UPDATE es atómico). */
    @Transactional
    public void reservar(Long id, int cantidad) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto " + id + " no existe"));

        int filasActualizadas = productoRepository.reservarStock(id, cantidad);
        if (filasActualizadas == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para el producto " + id);
        }

        int stockDespues = productoRepository.stockActual(producto.getId());
        evaluarAlerta(producto, stockDespues + cantidad, stockDespues);
    }

    /**
     * Avisa solo cuando el cambio hace que el producto CRUCE el umbral (de "normal" a "poco stock")
     * o cuando se agota. Así no se genera una alerta por cada cambio de un producto que ya estaba bajo,
     * ni cuando se edita otro dato (precio, nombre...) y el stock no cambia.
     *
     * @param stockAntes stock antes del cambio; null si el producto es nuevo
     */
    public void evaluarAlerta(Producto producto, Integer stockAntes, int stockDespues) {
        int umbral = props.umbralStockBajo();
        if (stockAntes != null && stockAntes == stockDespues) {
            return;
        }
        int antes = stockAntes != null ? stockAntes : umbral;

        boolean cruzoElUmbral = antes >= umbral && stockDespues < umbral;
        boolean seAgoto = stockDespues == 0;

        if (cruzoElUmbral || seAgoto) {
            eventPublisher.publishEvent(new AlertaStockEvento(
                    producto.getId(), producto.getNombre(), producto.getLocal().getId(),
                    stockDespues, umbral, Instant.now()));
        }
    }
}