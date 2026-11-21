package com.pedidos360.inventario.kafka.publisher;

import com.pedidos360.inventario.event.StockActualizadoEvento;
import com.pedidos360.inventario.kafka.config.KafkaTopicosProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba que el productor usa el tópico configurado, el id del producto como clave y no afecta si Kafka falla. */
class StockKafkaPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, StockActualizadoEvento> kafkaTemplate = mock(KafkaTemplate.class);

    private final KafkaTopicosProperties topicos = new KafkaTopicosProperties("pedidos.eventos", "inventario.stock");
    private final StockKafkaPublisher publisher = new StockKafkaPublisher(kafkaTemplate, topicos);

    private StockActualizadoEvento evento(int stock) {
        return new StockActualizadoEvento(11L, "Empanada de pino", 3L, stock, Instant.parse("2026-10-21T12:00:00Z"));
    }

    @Test
    void publicaEnElTopicoDeStockConElIdDelProductoComoClave() {
        StockActualizadoEvento evento = evento(27);
        when(kafkaTemplate.send(anyString(), anyString(), any(StockActualizadoEvento.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.alActualizarseElStock(evento);

        verify(kafkaTemplate).send("inventario.stock", "11", evento);
    }

    @Test
    void todosLosCambiosDeUnProductoUsanLaMismaClave() {
        StockActualizadoEvento primero = evento(27);
        StockActualizadoEvento segundo = evento(24);
        when(kafkaTemplate.send(anyString(), anyString(), any(StockActualizadoEvento.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.alActualizarseElStock(primero);
        publisher.alActualizarseElStock(segundo);

        verify(kafkaTemplate).send("inventario.stock", "11", primero);
        verify(kafkaTemplate).send("inventario.stock", "11", segundo);
    }

    @Test
    void siKafkaLanzaUnErrorNoSePropagaYLaCompraNoSeAfecta() {
        when(kafkaTemplate.send(anyString(), anyString(), any(StockActualizadoEvento.class)))
                .thenThrow(new IllegalStateException("Kafka caído"));

        assertDoesNotThrow(() -> publisher.alActualizarseElStock(evento(27)));
    }

    @Test
    void siElEnvioFallaDeFormaAsincronaTampocoSePropagaElError() {
        when(kafkaTemplate.send(anyString(), anyString(), any(StockActualizadoEvento.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("sin respuesta del broker")));

        assertDoesNotThrow(() -> publisher.alActualizarseElStock(evento(27)));
    }
}