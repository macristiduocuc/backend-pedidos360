package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.BindingResponse;
import com.pedidos360.rabbitadmin.dto.ColaRequest;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.dto.ExchangeRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeResponse;
import com.pedidos360.rabbitadmin.exception.RabbitAdminException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Toda la lógica para administrar RabbitMQ vive aquí: los controladores solo reciben la petición
 * y llaman a estos métodos, sin conocer nada de la librería de RabbitMQ.
 *
 * Reglas:
 *  - Los nombres de colas y exchanges solo admiten letras, números, punto, guion y guion bajo,
 *    y no pueden empezar con "amq." (reservado por RabbitMQ).
 *  - Los recursos "protegidos" (application.yml) no se pueden eliminar.
 *  - Eliminar es idempotente: si el recurso ya no existe, no es un error.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,255}$");
    private static final String PREFIJO_RESERVADO = "amq.";
    private static final int LARGO_MAXIMO = 255;

    private final AmqpAdmin amqpAdmin;
    private final RabbitAdminProperties props;

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitAdminProperties props) {
        this.amqpAdmin = amqpAdmin;
        this.props = props;
    }

    // ------------------------------------------------------------------ colas

    public ColaResponse crearCola(ColaRequest solicitud) {
        String nombre = solicitud.nombre();
        validarNombre(nombre, "la cola");

        boolean durable = solicitud.durable() == null || solicitud.durable();
        boolean quorum = solicitud.quorum() == null || solicitud.quorum();
        boolean hayDlx = tieneTexto(solicitud.deadLetterExchange());
        boolean hayDlxRoutingKey = tieneTexto(solicitud.deadLetterRoutingKey());

        if (quorum && !durable) {
            throw RabbitAdminException.solicitudInvalida("Una cola quorum debe ser durable");
        }
        if (hayDlxRoutingKey && !hayDlx) {
            throw RabbitAdminException.solicitudInvalida(
                    "Si indicas deadLetterRoutingKey, también debes indicar deadLetterExchange");
        }

        QueueBuilder constructor = durable ? QueueBuilder.durable(nombre) : QueueBuilder.nonDurable(nombre);
        if (quorum) {
            constructor = constructor.quorum();
        }
        if (hayDlx) {
            constructor = constructor.deadLetterExchange(solicitud.deadLetterExchange());
        }
        if (hayDlxRoutingKey) {
            constructor = constructor.deadLetterRoutingKey(solicitud.deadLetterRoutingKey());
        }
        Queue cola = constructor.build();

        ejecutar(() -> amqpAdmin.declareQueue(cola), "crear la cola '" + nombre + "'");
        log.info("Cola '{}' creada (durable={}, quorum={})", nombre, durable, quorum);
        return obtenerCola(nombre);
    }

    public ColaResponse obtenerCola(String nombre) {
        validarNombre(nombre, "la cola");
        QueueInformation info = ejecutar(() -> amqpAdmin.getQueueInfo(nombre), "consultar la cola '" + nombre + "'");
        if (info == null) {
            throw RabbitAdminException.noEncontrado("La cola '" + nombre + "' no existe");
        }
        return new ColaResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        validarNombre(nombre, "la cola");
        if (props.protegidos().colas().contains(nombre)) {
            throw RabbitAdminException.conflicto(
                    "La cola '" + nombre + "' es parte del sistema de pedidos y no se puede eliminar");
        }
        ejecutar(() -> amqpAdmin.deleteQueue(nombre), "eliminar la cola '" + nombre + "'");
        log.info("Cola '{}' eliminada", nombre);
    }

    // -------------------------------------------------------------- exchanges

    public ExchangeResponse crearExchange(ExchangeRequest solicitud) {
        String nombre = solicitud.nombre();
        validarNombre(nombre, "el exchange");
        boolean durable = solicitud.durable() == null || solicitud.durable();

        Exchange exchange = switch (solicitud.tipo()) {
            case DIRECT -> new DirectExchange(nombre, durable, false);
            case TOPIC -> new TopicExchange(nombre, durable, false);
            case FANOUT -> new FanoutExchange(nombre, durable, false);
            case HEADERS -> new HeadersExchange(nombre, durable, false);
        };

        ejecutar(() -> {
            amqpAdmin.declareExchange(exchange);
            return null;
        }, "crear el exchange '" + nombre + "'");
        log.info("Exchange '{}' creado (tipo={}, durable={})", nombre, solicitud.tipo(), durable);
        return new ExchangeResponse(nombre, solicitud.tipo(), durable);
    }

    public void eliminarExchange(String nombre) {
        validarNombre(nombre, "el exchange");
        if (props.protegidos().exchanges().contains(nombre)) {
            throw RabbitAdminException.conflicto(
                    "El exchange '" + nombre + "' es parte del sistema de pedidos y no se puede eliminar");
        }
        ejecutar(() -> amqpAdmin.deleteExchange(nombre), "eliminar el exchange '" + nombre + "'");
        log.info("Exchange '{}' eliminado", nombre);
    }

    // --------------------------------------------------------------- bindings

    public BindingResponse crearBinding(BindingRequest solicitud) {
        String exchange = exigirTexto(solicitud.exchange(), "El exchange");
        String cola = exigirTexto(solicitud.cola(), "La cola");
        String routingKey = routingKeyOVacia(solicitud.routingKey());

        QueueInformation infoCola = ejecutar(() -> amqpAdmin.getQueueInfo(cola), "consultar la cola '" + cola + "'");
        if (infoCola == null) {
            throw RabbitAdminException.noEncontrado("La cola '" + cola + "' no existe");
        }

        Binding binding = new Binding(cola, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        ejecutar(() -> {
            amqpAdmin.declareBinding(binding);
            return null;
        }, "crear el binding de '" + exchange + "' a '" + cola + "'");
        log.info("Binding creado: {} -> {} con routing key '{}'", exchange, cola, routingKey);
        return new BindingResponse(exchange, cola, routingKey);
    }

    public void eliminarBinding(String exchange, String cola, String routingKey) {
        String exchangeValido = exigirTexto(exchange, "El exchange");
        String colaValida = exigirTexto(cola, "La cola");
        String routingKeyValida = routingKeyOVacia(routingKey);

        if (props.protegidos().exchanges().contains(exchangeValido)
                && props.protegidos().colas().contains(colaValida)) {
            throw RabbitAdminException.conflicto("El binding de '" + exchangeValido + "' a '" + colaValida
                    + "' es parte del sistema de pedidos y no se puede eliminar");
        }

        Binding binding = new Binding(colaValida, Binding.DestinationType.QUEUE,
                exchangeValido, routingKeyValida, null);
        ejecutar(() -> {
            amqpAdmin.removeBinding(binding);
            return null;
        }, "eliminar el binding de '" + exchangeValido + "' a '" + colaValida + "'");
        log.info("Binding eliminado: {} -> {} con routing key '{}'", exchangeValido, colaValida, routingKeyValida);
    }

    // ---------------------------------------------------------------- ayudas

    private void validarNombre(String nombre, String recurso) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw RabbitAdminException.solicitudInvalida("El nombre de " + recurso
                    + " no es válido: usa solo letras, números, punto, guion y guion bajo (máximo 255 caracteres)");
        }
        if (nombre.startsWith(PREFIJO_RESERVADO)) {
            throw RabbitAdminException.solicitudInvalida(
                    "Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
    }

    private String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw RabbitAdminException.solicitudInvalida(campo + " es obligatorio");
        }
        if (valor.length() > LARGO_MAXIMO) {
            throw RabbitAdminException.solicitudInvalida(campo + " no puede superar los 255 caracteres");
        }
        return valor;
    }

    private String routingKeyOVacia(String routingKey) {
        if (routingKey == null) {
            return "";
        }
        if (routingKey.length() > LARGO_MAXIMO) {
            throw RabbitAdminException.solicitudInvalida("La routing key no puede superar los 255 caracteres");
        }
        return routingKey;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Ejecuta la operación contra RabbitMQ y traduce los errores de la librería a errores claros de la API. */
    private <T> T ejecutar(Supplier<T> operacion, String descripcion) {
        try {
            return operacion.get();
        } catch (AmqpConnectException ex) {
            throw RabbitAdminException.noDisponible(
                    "No hay conexión con RabbitMQ; no se pudo " + descripcion);
        } catch (AmqpException ex) {
            String causa = mensajeCompleto(ex);
            if (causa.contains("PRECONDITION_FAILED")) {
                throw RabbitAdminException.conflicto(
                        "Ya existe con otra configuración; no se pudo " + descripcion);
            }
            if (causa.contains("NOT_FOUND")) {
                throw RabbitAdminException.noEncontrado(
                        "No existe el exchange o la cola indicada; no se pudo " + descripcion);
            }
            log.error("RabbitMQ rechazó la operación '{}': {}", descripcion, causa);
            throw RabbitAdminException.errorBroker("RabbitMQ rechazó la operación: no se pudo " + descripcion);
        }
    }

    private String mensajeCompleto(Throwable error) {
        StringBuilder texto = new StringBuilder();
        Throwable actual = error;
        for (int profundidad = 0; actual != null && profundidad < 10; profundidad++) {
            if (actual.getMessage() != null) {
                texto.append(actual.getMessage()).append(' ');
            }
            actual = actual.getCause();
        }
        return texto.toString();
    }
}
