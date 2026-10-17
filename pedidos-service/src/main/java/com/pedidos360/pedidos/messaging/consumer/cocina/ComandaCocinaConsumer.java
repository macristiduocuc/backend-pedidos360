package com.pedidos360.pedidos.messaging.consumer.cocina;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.config.ConvertidorCrudoRabbitConfig;
import com.pedidos360.pedidos.messaging.consumer.ConsumidorRabbit;
import com.pedidos360.pedidos.messaging.consumer.ProcesadorMensajes;
import com.pedidos360.pedidos.service.ComandaService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Dominio cocina: genera el ticket de comanda cuando llega un pedido nuevo. */
@Component
public class ComandaCocinaConsumer extends ConsumidorRabbit {

    private final ComandaService comandaService;

    public ComandaCocinaConsumer(ProcesadorMensajes procesador, ComandaService comandaService) {
        super(procesador);
        this.comandaService = comandaService;
    }

    @RabbitListener(queues = "${app.mensajeria.comanda.cola}",
            messageConverter = ConvertidorCrudoRabbitConfig.NOMBRE_BEAN)
    public void alRecibirPedidoCreado(Message mensaje, Channel canal) {
        consumir(mensaje, canal, PedidoEvento.class, comandaService::registrarComanda);
    }
}