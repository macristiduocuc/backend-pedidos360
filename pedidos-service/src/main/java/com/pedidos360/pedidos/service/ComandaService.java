package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.pedidos.model.EventoAuditoria;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.EventoAuditoriaRepository;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/** Genera el ticket de comanda de cocina a partir del evento de pedido creado. */
@Service
public class ComandaService {

    private static final Logger log = LoggerFactory.getLogger(ComandaService.class);

    private final PedidoRepository pedidoRepository;
    private final EventoAuditoriaRepository eventoRepository;

    public ComandaService(PedidoRepository pedidoRepository, EventoAuditoriaRepository eventoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional
    public void registrarComanda(PedidoEvento evento) {
        if (evento == null || evento.pedidoId() == null) {
            throw new MensajeNoProcesableException("El evento no trae el id del pedido");
        }
        if (evento.tipo() != PedidoEvento.Tipo.CREADO) {
            throw new MensajeNoProcesableException("La comanda solo se genera para pedidos creados");
        }

        Pedido pedido = pedidoRepository.findById(evento.pedidoId())
                .orElseThrow(() -> new MensajeNoProcesableException(
                        "El pedido " + evento.pedidoId() + " no existe"));

        String detalle = pedido.getItems().stream()
                .map(item -> item.getCantidad() + " x " + item.getNombreProducto())
                .collect(Collectors.joining(", "));

        String descripcion = "Comanda del pedido #" + pedido.getId()
                + " (local " + pedido.getLocalId() + ", " + pedido.getModalidad() + "): " + detalle + ".";

        eventoRepository.save(new EventoAuditoria(pedido.getId(), descripcion, "cocina"));
        log.info("Comanda generada para el pedido {}", pedido.getId());
    }
}