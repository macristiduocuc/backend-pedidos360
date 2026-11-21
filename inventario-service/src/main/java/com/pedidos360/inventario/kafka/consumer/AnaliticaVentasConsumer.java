package com.pedidos360.inventario.kafka.consumer;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.service.AnaliticaVentasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "analitica-ventas": lee TODOS los eventos de pedidos y calcula las unidades vendidas
 * por producto. Es un grupo distinto al de auditoría (pedidos-service), así que los dos reciben cada evento
 * y cada uno cumple su propia responsabilidad. La lógica está en AnaliticaVentasService.
 */
@Component
public class AnaliticaVentasConsumer {

    private final AnaliticaVentasService analiticaService;

    public AnaliticaVentasConsumer(AnaliticaVentasService analiticaService) {
        this.analiticaService = analiticaService;
    }

    @KafkaListener(topics = "${app.kafka.topicos.pedidos-eventos}", groupId = "${app.kafka.grupos.analitica-ventas}")
    public void alRecibirEventoDePedido(ConsumerRecord<String, PedidoEvento> registro) {
        analiticaService.registrar(registro);
    }
}