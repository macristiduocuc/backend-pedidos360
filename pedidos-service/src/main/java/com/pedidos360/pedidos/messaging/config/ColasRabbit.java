package com.pedidos360.pedidos.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/**
 * Utilidades compartidas por las clases de configuración de cada caso de uso,
 * para que todas las colas se declaren con el mismo criterio.
 */
final class ColasRabbit {

    private ColasRabbit() {
    }

    /**
     * Cola principal: durable, de tipo quorum (se replica entre los nodos del clúster)
     * y con dead-letter hacia pedidos.dlx. Lo que se rechaza sin reencolar termina en la DLQ.
     */
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