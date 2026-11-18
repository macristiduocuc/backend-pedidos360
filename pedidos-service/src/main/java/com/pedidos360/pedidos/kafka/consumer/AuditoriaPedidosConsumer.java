package com.pedidos360.pedidos.kafka.consumer;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.service.AuditoriaKafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "auditoria": lee TODOS los eventos del tópico pedidos.eventos y los guarda
 * como historial. La lógica está en AuditoriaKafkaService; esta clase solo recibe el mensaje.
 * Los errores los maneja KafkaConsumidorConfig.
 */
@Component
public class AuditoriaPedidosConsumer {

    private final AuditoriaKafkaService auditoriaService;

    public AuditoriaPedidosConsumer(AuditoriaKafkaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @KafkaListener(topics = "${app.kafka.topicos.pedidos-eventos}", groupId = "${app.kafka.grupos.auditoria}")
    public void alRecibirEventoDePedido(ConsumerRecord<String, PedidoEvento> registro) {
        auditoriaService.registrar(registro);
    }
}