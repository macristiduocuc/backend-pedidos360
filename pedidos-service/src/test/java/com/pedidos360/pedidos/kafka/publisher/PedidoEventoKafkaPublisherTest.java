package com.pedidos360.pedidos.kafka.publisher;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.kafka.config.KafkaTopicosProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba que el productor de Kafka usa el tópico configurado, la clave correcta y no afecta al pedido si falla. */
class PedidoEventoKafkaPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, PedidoEvento> kafkaTemplate = mock(KafkaTemplate.class);

    private final KafkaTopicosProperties topicos = new KafkaTopicosProperties("pedidos.eventos");
    private final PedidoEventoKafkaPublisher publisher = new PedidoEventoKafkaPublisher(kafkaTemplate, topicos);

    private PedidoEvento evento(PedidoEvento.Tipo tipo, String estado) {
        return new PedidoEvento(tipo, 7L, 3L, estado, new BigDecimal("1990.00"),
                Instant.parse("2026-10-20T12:00:00Z"), List.of(new PedidoEvento.Item(11L, 2)));
    }

    @Test
    void publicaEnElTopicoConfiguradoConElIdDelPedidoComoClave() {
        PedidoEvento evento = evento(PedidoEvento.Tipo.CREADO, "Pendiente");
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEvento.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.alOcurrirEvento(evento);

        verify(kafkaTemplate).send("pedidos.eventos", "7", evento);
    }

    @Test
    void todosLosEventosDeUnPedidoUsanLaMismaClaveParaMantenerElOrden() {
        PedidoEvento creado = evento(PedidoEvento.Tipo.CREADO, "Pendiente");
        PedidoEvento preparando = evento(PedidoEvento.Tipo.ESTADO_CAMBIADO, "En preparación");
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEvento.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.alOcurrirEvento(creado);
        publisher.alOcurrirEvento(preparando);

        verify(kafkaTemplate).send("pedidos.eventos", "7", creado);
        verify(kafkaTemplate).send("pedidos.eventos", "7", preparando);
    }

    @Test
    void siKafkaLanzaUnErrorAlEnviarNoSePropagaYElPedidoNoSeAfecta() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEvento.class)))
                .thenThrow(new IllegalStateException("Kafka caído"));

        assertDoesNotThrow(() -> publisher.alOcurrirEvento(evento(PedidoEvento.Tipo.CREADO, "Pendiente")));
    }

    @Test
    void siElEnvioFallaDeFormaAsincronaTampocoSePropagaElError() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEvento.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("sin respuesta del broker")));

        assertDoesNotThrow(() -> publisher.alOcurrirEvento(evento(PedidoEvento.Tipo.CANCELADO, "Cancelado")));
    }
}