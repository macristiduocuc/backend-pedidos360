package com.pedidos360.pedidos.kafka.publisher;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.kafka.config.KafkaTopicosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Productor de Kafka: publica en el tópico pedidos.eventos los mismos eventos de pedido que
 * PedidoEventoPublisher envía a RabbitMQ. Kafka COMPLEMENTA a RabbitMQ: cada uno hace su trabajo
 * (RabbitMQ reparte tareas; Kafka conserva el historial) y ninguno depende del otro.
 *
 * - La clave del mensaje es el id del pedido: todos los eventos de un mismo pedido caen en la misma
 *   partición, así que se leen en el orden en que ocurrieron.
 * - Se ejecuta DESPUÉS del commit (AFTER_COMMIT): nunca se publica un evento de un pedido que no quedó guardado.
 * - Si Kafka falla, solo se deja un error en el log: el pedido y los mensajes de RabbitMQ siguen igual.
 */
@Component
public class PedidoEventoKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventoKafkaPublisher.class);

    private final KafkaTemplate<String, PedidoEvento> kafkaTemplate;
    private final KafkaTopicosProperties topicos;

    public PedidoEventoKafkaPublisher(KafkaTemplate<String, PedidoEvento> kafkaTemplate,
                                      KafkaTopicosProperties topicos) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicos = topicos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alOcurrirEvento(PedidoEvento evento) {
        String clave = String.valueOf(evento.pedidoId());
        try {
            kafkaTemplate.send(topicos.pedidosEventos(), clave, evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("No se pudo publicar el evento {} del pedido {} en Kafka (el pedido igual quedó guardado): {}",
                                    evento.tipo(), evento.pedidoId(), error.getMessage());
                        } else if (resultado != null && resultado.getRecordMetadata() != null) {
                            log.info("Evento {} del pedido {} publicado en Kafka (partición {}, offset {})",
                                    evento.tipo(), evento.pedidoId(),
                                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el evento {} del pedido {} en Kafka (el pedido igual quedó guardado): {}",
                    evento.tipo(), evento.pedidoId(), ex.getMessage());
        }
    }
}