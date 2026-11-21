package com.pedidos360.inventario.kafka.publisher;

import com.pedidos360.inventario.event.StockActualizadoEvento;
import com.pedidos360.inventario.kafka.config.KafkaTopicosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Productor de Kafka: publica en inventario.stock el stock actual de un producto cada vez que cambia.
 *
 * - La clave es el id del producto: el tópico está compactado, así que Kafka conserva solo el último
 *   valor de cada producto, y los cambios de un mismo producto llegan en orden.
 * - Se ejecuta DESPUÉS del commit (AFTER_COMMIT): nunca se publica un stock que no quedó guardado.
 * - Si Kafka falla, solo se deja un error en el log: la compra o la reposición siguen igual.
 */
@Component
public class StockKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockKafkaPublisher.class);

    private final KafkaTemplate<String, StockActualizadoEvento> kafkaTemplate;
    private final KafkaTopicosProperties topicos;

    public StockKafkaPublisher(KafkaTemplate<String, StockActualizadoEvento> kafkaTemplate,
                               KafkaTopicosProperties topicos) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicos = topicos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alActualizarseElStock(StockActualizadoEvento evento) {
        String clave = String.valueOf(evento.productoId());
        try {
            kafkaTemplate.send(topicos.inventarioStock(), clave, evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("No se pudo publicar el stock del producto {} en Kafka (el cambio igual quedó guardado): {}",
                                    evento.productoId(), error.getMessage());
                        } else if (resultado != null && resultado.getRecordMetadata() != null) {
                            log.info("Stock del producto {} ({}) publicado en Kafka (partición {}, offset {})",
                                    evento.productoId(), evento.stock(),
                                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el stock del producto {} en Kafka (el cambio igual quedó guardado): {}",
                    evento.productoId(), ex.getMessage());
        }
    }
}