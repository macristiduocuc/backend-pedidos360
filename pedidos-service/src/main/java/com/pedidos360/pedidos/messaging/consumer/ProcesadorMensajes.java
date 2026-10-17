package com.pedidos360.pedidos.messaging.consumer;

import com.pedidos360.pedidos.messaging.config.ConsumidorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Convierte el mensaje y ejecuta la acción de negocio aplicando la política de errores:
 *
 *  1. Contenido ilegible o dato que no corresponde  -> DESCARTADO (DLQ), sin reintentos.
 *  2. MensajeNoProcesableException (irrecuperable)   -> DESCARTADO (DLQ), sin reintentos.
 *  3. Cualquier otro error (recuperable, ej. BD caída) -> se reintenta hasta max-intentos
 *     y, si sigue fallando, DESCARTADO (DLQ).
 *  4. Servicio interrumpido mientras espera          -> REENCOLAR (no se pierde el mensaje).
 *
 * Todo lo que se descarta queda registrado en el log con su motivo.
 * Quien confirma o rechaza ante RabbitMQ (ack/nack) es ConsumidorRabbit.
 */
@Component
public class ProcesadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorMensajes.class);

    private final MessageConverter converter;
    private final ConsumidorProperties props;

    public ProcesadorMensajes(MessageConverter converter, ConsumidorProperties props) {
        this.converter = converter;
        this.props = props;
    }

    public <T> ResultadoProceso procesar(Message mensaje, Class<T> tipo, Consumer<T> accion) {
        String cola = mensaje.getMessageProperties().getConsumerQueue();

        T contenido;
        try {
            contenido = leer(mensaje, tipo);
        } catch (MensajeNoProcesableException ex) {
            log.error("Mensaje descartado hacia la DLQ (cola {}): {}", cola, ex.getMessage());
            return ResultadoProceso.DESCARTADO;
        }

        int maxIntentos = props.maxIntentos();
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                accion.accept(contenido);
                return ResultadoProceso.EXITO;
            } catch (MensajeNoProcesableException ex) {
                log.error("Mensaje irrecuperable descartado hacia la DLQ (cola {}): {}", cola, ex.getMessage());
                return ResultadoProceso.DESCARTADO;
            } catch (RuntimeException ex) {
                if (intento == maxIntentos) {
                    log.error("Mensaje descartado hacia la DLQ tras {} intentos fallidos (cola {}): {}",
                            intento, cola, ex.toString());
                    return ResultadoProceso.DESCARTADO;
                }
                log.warn("Intento {}/{} fallido (cola {}); se reintenta: {}",
                        intento, maxIntentos, cola, ex.toString());
                if (!esperar()) {
                    log.warn("Servicio interrumpido durante el reintento (cola {}); se devuelve el mensaje a la cola", cola);
                    return ResultadoProceso.REENCOLAR;
                }
            }
        }
        return ResultadoProceso.DESCARTADO;
    }

    /** Convierte el JSON al tipo esperado. Cualquier problema aquí es irrecuperable. */
    private <T> T leer(Message mensaje, Class<T> tipo) {
        // Igual que hace Spring con @RabbitListener: indica el tipo esperado para deserializar.
        mensaje.getMessageProperties().setInferredArgumentType(tipo);
        Object convertido;
        try {
            convertido = converter.fromMessage(mensaje);
        } catch (RuntimeException ex) {
            throw new MensajeNoProcesableException(
                    "El contenido no es un JSON válido para " + tipo.getSimpleName(), ex);
        }
        if (!tipo.isInstance(convertido)) {
            throw new MensajeNoProcesableException(
                    "Contenido inesperado: se esperaba " + tipo.getSimpleName()
                            + " en JSON (content-type application/json)");
        }
        return tipo.cast(convertido);
    }

    private boolean esperar() {
        try {
            Thread.sleep(props.esperaReintento().toMillis());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}