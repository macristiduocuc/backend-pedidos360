package com.pedidos360.inventario.service;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.inventario.model.ReposicionProcesada;
import com.pedidos360.inventario.repository.ProductoRepository;
import com.pedidos360.inventario.repository.ReposicionProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Devuelve al stock los productos de un pedido cancelado.
 *
 * Es idempotente: RabbitMQ garantiza "al menos una entrega", así que un mismo mensaje puede
 * llegar dos veces. El pedido se anota en reposiciones_procesadas y, si vuelve a llegar,
 * se ignora (no se repone dos veces). Todo ocurre en una sola transacción: o se reponen
 * todos los productos y se anota el pedido, o no se hace nada.
 */
@Service
public class ReposicionStockService {

    private static final Logger log = LoggerFactory.getLogger(ReposicionStockService.class);

    private final ProductoRepository productoRepository;
    private final ReposicionProcesadaRepository procesadaRepository;

    public ReposicionStockService(ProductoRepository productoRepository,
                                  ReposicionProcesadaRepository procesadaRepository) {
        this.productoRepository = productoRepository;
        this.procesadaRepository = procesadaRepository;
    }

    @Transactional
    public void reponer(PedidoEvento evento) {
        if (evento == null || evento.pedidoId() == null) {
            throw new MensajeNoProcesableException("El evento no trae el id del pedido");
        }
        if (evento.tipo() != PedidoEvento.Tipo.CANCELADO) {
            throw new MensajeNoProcesableException("La reposición solo corresponde a pedidos cancelados");
        }
        if (procesadaRepository.existsById(evento.pedidoId())) {
            log.info("El stock del pedido {} ya estaba repuesto; se ignora el mensaje duplicado", evento.pedidoId());
            return;
        }

        List<PedidoEvento.Item> items = evento.items() == null ? List.of() : evento.items();
        for (PedidoEvento.Item item : items) {
            if (item.productoId() == null || item.cantidad() == null || item.cantidad() < 1) {
                throw new MensajeNoProcesableException("El pedido " + evento.pedidoId() + " trae un ítem inválido");
            }
            int filas = productoRepository.reponerStock(item.productoId(), item.cantidad());
            if (filas == 0) {
                throw new MensajeNoProcesableException("El producto " + item.productoId() + " no existe");
            }
        }

        procesadaRepository.save(new ReposicionProcesada(evento.pedidoId()));
        log.info("Stock repuesto del pedido {} ({} productos)", evento.pedidoId(), items.size());
    }
}