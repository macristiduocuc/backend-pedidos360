package com.pedidos360.inventario.messaging.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Base de todos los consumidores. Aquí se confirma ante RabbitMQ, de forma explícita,
 * qué pasó con cada mensaje (la cola usa ACK manual):
 *
 *  - EXITO      -> basicAck: el mensaje se elimina de la cola.
 *  - DESCARTADO -> basicNack sin reencolar: RabbitMQ lo envía a la DLQ de esa cola.
 *  - REENCOLAR  -> basicNack con reencolado: el mensaje vuelve a la cola.
 */
public abstract class ConsumidorRabbit {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorRabbit.class);

    private final ProcesadorMensajes procesador;

    protected ConsumidorRabbit(ProcesadorMensajes procesador) {
        this.procesador = procesador;
    }

    protected <T> void consumir(Message mensaje, Channel canal, Class<T> tipo, Consumer<T> accion) {
        long tag = mensaje.getMessageProperties().getDeliveryTag();
        ResultadoProceso resultado;
        try {
            resultado = procesador.procesar(mensaje, tipo, accion);
        } catch (RuntimeException ex) {
            // Red de seguridad: pase lo que pase, el mensaje se confirma o se rechaza (nunca queda colgado).
            log.error("Error inesperado procesando el mensaje {}; se envía a la DLQ: {}", tag, ex.toString());
            resultado = ResultadoProceso.DESCARTADO;
        }
        try {
            switch (resultado) {
                case EXITO -> canal.basicAck(tag, false);
                case DESCARTADO -> canal.basicNack(tag, false, false);
                case REENCOLAR -> canal.basicNack(tag, false, true);
            }
        } catch (IOException ex) {
            log.error("No se pudo informar a RabbitMQ el resultado {} del mensaje {}: {}",
                    resultado, tag, ex.getMessage());
        }
    }
}