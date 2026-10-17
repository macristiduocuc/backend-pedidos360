package com.pedidos360.pedidos.messaging.publisher;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.config.MensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Productor: toma los eventos que anuncia PedidoService y los publica en pedidos.exchange.
 *
 * - Se ejecuta DESPUÉS de que la transacción se confirmó (AFTER_COMMIT): nunca se publica
 *   un evento de un pedido que no quedó guardado en la base de datos.
 * - Si RabbitMQ falla, solo se deja un error en el log: el pedido ya está guardado y la
 *   operación del usuario se completa igual (la mensajería no afecta la lógica existente).
 */
@Component
public class PedidoEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoPublisher.class);
    private static final String MARCADOR_LOCAL = "{localId}";

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public PedidoEventoPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alOcurrirEvento(PedidoEvento evento) {
        String routingKey = routingKeyPara(evento);
        try {
            rabbitTemplate.convertAndSend(props.exchange().principal(), routingKey, evento);
            log.info("Evento {} del pedido {} publicado con routing key {}",
                    evento.tipo(), evento.pedidoId(), routingKey);
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el evento {} del pedido {} en RabbitMQ (el pedido igual quedó guardado): {}",
                    evento.tipo(), evento.pedidoId(), ex.getMessage());
        }
    }

    private String routingKeyPara(PedidoEvento evento) {
        String plantilla = switch (evento.tipo()) {
            case CREADO -> props.routingKey().pedidoCreado();
            case ESTADO_CAMBIADO -> props.routingKey().pedidoEstado();
            case CANCELADO -> props.routingKey().pedidoCancelado();
        };
        return plantilla.replace(MARCADOR_LOCAL, String.valueOf(evento.localId()));
    }
}