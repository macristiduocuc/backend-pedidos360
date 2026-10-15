package com.pedidos360.pedidos.messaging.consumer.notificacion;

import com.pedidos360.pedidos.event.PedidoEvento;
import com.pedidos360.pedidos.messaging.config.ConvertidorCrudoRabbitConfig;
import com.pedidos360.pedidos.messaging.consumer.ConsumidorRabbit;
import com.pedidos360.pedidos.messaging.consumer.ProcesadorMensajes;
import com.pedidos360.pedidos.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Dominio notificaciones: avisa al cliente cuando su pedido se crea, cambia de estado o se cancela. */
@Component
public class NotificacionClienteConsumer extends ConsumidorRabbit {

    private final NotificacionService notificacionService;

    public NotificacionClienteConsumer(ProcesadorMensajes procesador, NotificacionService notificacionService) {
        super(procesador);
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = "${app.mensajeria.notificacion-cliente.cola}",
            messageConverter = ConvertidorCrudoRabbitConfig.NOMBRE_BEAN)
    public void alRecibirEventoDePedido(Message mensaje, Channel canal) {
        consumir(mensaje, canal, PedidoEvento.class, notificacionService::notificarCliente);
    }
}