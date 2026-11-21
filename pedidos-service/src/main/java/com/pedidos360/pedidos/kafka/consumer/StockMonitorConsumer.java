package com.pedidos360.pedidos.kafka.consumer;

import com.pedidos360.pedidos.event.StockActualizadoEvento;
import com.pedidos360.pedidos.service.StockMonitorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor del grupo "stock-monitor": lee el tópico inventario.stock y mantiene el último stock conocido
 * de cada producto. Este tópico lleva otro tipo de mensaje que pedidos.eventos, por eso el listener indica
 * su propio tipo (el del resto de consumidores es PedidoEvento). La lógica está en StockMonitorService.
 */
@Component
public class StockMonitorConsumer {

    private final StockMonitorService stockMonitorService;

    public StockMonitorConsumer(StockMonitorService stockMonitorService) {
        this.stockMonitorService = stockMonitorService;
    }

    @KafkaListener(
            topics = "${app.kafka.topicos.inventario-stock}",
            groupId = "${app.kafka.grupos.stock-monitor}",
            properties = "spring.json.value.default.type=com.pedidos360.pedidos.event.StockActualizadoEvento")
    public void alRecibirStockActualizado(ConsumerRecord<String, StockActualizadoEvento> registro) {
        stockMonitorService.registrar(registro);
    }
}