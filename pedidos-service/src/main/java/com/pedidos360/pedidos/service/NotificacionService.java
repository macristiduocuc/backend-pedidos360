package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.pedidos.model.Notificacion;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.NotificacionRepository;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la notificación al cliente. El correo NO viaja en el mensaje de RabbitMQ:
 * se busca aquí, en la base de datos del backend, usando el id del pedido.
 * El "envío" es simulado: queda guardado en la tabla notificaciones.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final PedidoRepository pedidoRepository;
    private final NotificacionRepository notificacionRepository;

    public NotificacionService(PedidoRepository pedidoRepository, NotificacionRepository notificacionRepository) {
        this.pedidoRepository = pedidoRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional
    public void notificarCliente(PedidoEvento evento) {
        if (evento == null || evento.pedidoId() == null || evento.tipo() == null) {
            throw new MensajeNoProcesableException("El evento no trae el id del pedido o su tipo");
        }

        Pedido pedido = pedidoRepository.findById(evento.pedidoId())
                .orElseThrow(() -> new MensajeNoProcesableException(
                        "El pedido " + evento.pedidoId() + " no existe"));

        notificacionRepository.save(
                Notificacion.paraCliente(pedido.getId(), pedido.getClienteCorreo(), textoPara(evento, pedido.getId())));
        log.info("Notificación registrada para el cliente del pedido {} ({})", pedido.getId(), evento.tipo());
    }

    private String textoPara(PedidoEvento evento, Long pedidoId) {
        return switch (evento.tipo()) {
            case CREADO -> "Recibimos tu pedido #" + pedidoId + ".";
            case ESTADO_CAMBIADO -> "Tu pedido #" + pedidoId + " ahora está: " + evento.estado() + ".";
            case CANCELADO -> "Tu pedido #" + pedidoId + " fue cancelado.";
        };
    }
}