package com.pedidos360.inventario.service;

import com.pedidos360.inventario.dto.ProductoVendidoResponse;
import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.kafka.consumer.EventoInvalidoException;
import com.pedidos360.inventario.model.VentaRegistrada;
import com.pedidos360.inventario.repository.VentaProductoRepository;
import com.pedidos360.inventario.repository.VentaRegistradaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Analítica de ventas alimentada desde Kafka: cuenta las unidades vendidas por producto.
 *
 *  - Pedido CREADO: suma las unidades de cada producto (una sola vez por pedido).
 *  - Pedido CANCELADO: resta esas unidades (una sola vez).
 *  - Cambios de estado: no afectan la cantidad vendida, se ignoran.
 *
 * Los eventos de un mismo pedido llegan en orden porque comparten clave (y por tanto partición):
 * por eso el CREADO siempre se procesa antes que el CANCELADO.
 */
@Service
public class AnaliticaVentasService {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaVentasService.class);
    private static final int LIMITE_MAXIMO = 50;

    private final VentaProductoRepository ventasPorProducto;
    private final VentaRegistradaRepository ventasRegistradas;

    public AnaliticaVentasService(VentaProductoRepository ventasPorProducto,
                                  VentaRegistradaRepository ventasRegistradas) {
        this.ventasPorProducto = ventasPorProducto;
        this.ventasRegistradas = ventasRegistradas;
    }

    @Transactional
    public void registrar(ConsumerRecord<String, PedidoEvento> registro) {
        PedidoEvento evento = registro.value();
        if (evento == null || evento.pedidoId() == null || evento.tipo() == null) {
            throw new EventoInvalidoException("El evento no trae el id del pedido o su tipo");
        }
        switch (evento.tipo()) {
            case CREADO -> contarVenta(evento);
            case CANCELADO -> descontarVenta(evento);
            default -> log.debug("Evento {} del pedido {} ignorado: no cambia lo vendido", evento.tipo(), evento.pedidoId());
        }
    }

    private void contarVenta(PedidoEvento evento) {
        if (ventasRegistradas.existsById(evento.pedidoId())) {
            log.info("El pedido {} ya estaba contado; se ignora el mensaje duplicado", evento.pedidoId());
            return;
        }
        for (PedidoEvento.Item item : itemsValidos(evento)) {
            ventasPorProducto.sumarUnidades(item.productoId(), item.cantidad());
        }
        ventasRegistradas.save(new VentaRegistrada(evento.pedidoId()));
        log.info("Analítica: pedido {} contado", evento.pedidoId());
    }

    private void descontarVenta(PedidoEvento evento) {
        Optional<VentaRegistrada> venta = ventasRegistradas.findById(evento.pedidoId());
        if (venta.isEmpty()) {
            log.warn("Se canceló el pedido {}, que no estaba contado; se ignora", evento.pedidoId());
            return;
        }
        if (venta.get().isCancelada()) {
            log.info("La cancelación del pedido {} ya estaba descontada; se ignora", evento.pedidoId());
            return;
        }
        for (PedidoEvento.Item item : itemsValidos(evento)) {
            ventasPorProducto.sumarUnidades(item.productoId(), -item.cantidad().longValue());
        }
        venta.get().marcarCancelada();
        ventasRegistradas.save(venta.get());
        log.info("Analítica: pedido {} descontado por cancelación", evento.pedidoId());
    }

    private List<PedidoEvento.Item> itemsValidos(PedidoEvento evento) {
        List<PedidoEvento.Item> items = evento.items() == null ? List.of() : evento.items();
        for (PedidoEvento.Item item : items) {
            if (item.productoId() == null || item.cantidad() == null || item.cantidad() < 1) {
                throw new EventoInvalidoException("El pedido " + evento.pedidoId() + " trae un ítem inválido");
            }
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<ProductoVendidoResponse> productosMasVendidos(int limite) {
        int tamano = Math.min(Math.max(limite, 1), LIMITE_MAXIMO);
        return ventasPorProducto.masVendidos(PageRequest.of(0, tamano));
    }
}