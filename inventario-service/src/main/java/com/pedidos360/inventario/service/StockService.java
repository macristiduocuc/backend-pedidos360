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
 * Reserva de stock. Es la misma lógica que antes vivía en ProductoController, con una
 * diferencia: cuando la reserva deja el producto con poco stock, anuncia una alerta
 * (sin conocer RabbitMQ: quien la publica es AlertaStockPublisher, tras el commit).
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

        anunciarAlertaSiCorresponde(producto, cantidad);
    }

    /**
     * Avisa solo cuando la reserva hace que el producto CRUCE el umbral (de "normal" a "poco stock")
     * o cuando se agota. Así no se genera una alerta por cada compra de un producto que ya estaba bajo.
     */
    private void anunciarAlertaSiCorresponde(Producto producto, int cantidad) {
        int umbral = props.umbralStockBajo();
        int stockDespues = productoRepository.stockActual(producto.getId());
        int stockAntes = stockDespues + cantidad;

        boolean cruzoElUmbral = stockAntes >= umbral && stockDespues < umbral;
        boolean seAgoto = stockDespues == 0;

        if (cruzoElUmbral || seAgoto) {
            eventPublisher.publishEvent(new AlertaStockEvento(
                    producto.getId(), producto.getNombre(), producto.getLocal().getId(),
                    stockDespues, umbral, Instant.now()));
        }
    }
}