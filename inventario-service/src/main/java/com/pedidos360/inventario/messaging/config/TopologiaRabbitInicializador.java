package com.pedidos360.inventario.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Abre una conexión a RabbitMQ al arrancar para que Spring declare de inmediato los
 * exchanges, colas y bindings. Si RabbitMQ no está disponible, solo deja un aviso en el
 * log: el servicio de inventario sigue funcionando igual (la mensajería no debe afectar
 * la lógica existente).
 */
@Component
public class TopologiaRabbitInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopologiaRabbitInicializador.class);

    private final ConnectionFactory connectionFactory;

    public TopologiaRabbitInicializador(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Connection conexion = connectionFactory.createConnection();
            conexion.close();
            log.info("RabbitMQ: conexión establecida y topología declarada (exchanges, colas y DLQ).");
        } catch (Exception ex) {
            log.warn("RabbitMQ no disponible al iniciar; la topología se declarará al primer uso. Causa: {}",
                    ex.getMessage());
        }
    }
}