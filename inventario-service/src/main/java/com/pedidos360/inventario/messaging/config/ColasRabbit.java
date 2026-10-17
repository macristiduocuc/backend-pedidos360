package com.pedidos360.inventario.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/** Utilidades para declarar todas las colas con el mismo criterio. */
final class ColasRabbit {

    private ColasRabbit() {
    }

    /** Cola principal: durable, quorum (se replica entre los nodos) y con dead-letter hacia el DLX. */
    static Queue colaPrincipal(String nombre, String dlx, String nombreDlq) {
        return QueueBuilder.durable(nombre)
                .quorum()
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(nombreDlq)
                .build();
    }

    /** DLQ: donde quedan los mensajes que no pudieron procesarse. */
    static Queue colaDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .quorum()
                .build();
    }

    /** Enlaza una DLQ al exchange de fallidos usando su propio nombre como routing key. */
    static Binding bindingDlq(Queue dlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlq).to(dlxExchange).with(dlq.getName());
    }
}