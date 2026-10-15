package com.pedidos360.inventario.messaging.publisher;

import com.pedidos360.inventario.event.AlertaStockEvento;
import com.pedidos360.inventario.messaging.config.MensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Productor: publica en pedidos.exchange las alertas de stock bajo que anuncia StockService.
 *
 * - Se ejecuta DESPUÉS de que la transacción se confirmó (AFTER_COMMIT): nunca se avisa de una
 *   reserva que no quedó guardada.
 * - Si RabbitMQ falla, solo se deja un error en el log: la reserva ya está hecha y la compra
 *   se completa igual (la mensajería no afecta la lógica existente).
 */
@Component
public class AlertaStockPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertaStockPublisher.class);
    private static final String MARCADOR_LOCAL = "{localId}";

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public AlertaStockPublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alOcurrirAlerta(AlertaStockEvento evento) {
        String routingKey = props.routingKey().stockBajo().replace(MARCADOR_LOCAL, String.valueOf(evento.localId()));
        try {
            rabbitTemplate.convertAndSend(props.exchange().principal(), routingKey, evento);
            log.info("Alerta de stock bajo del producto {} publicada con routing key {}",
                    evento.productoId(), routingKey);
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar la alerta de stock bajo del producto {} en RabbitMQ (la reserva igual quedó hecha): {}",
                    evento.productoId(), ex.getMessage());
        }
    }
}