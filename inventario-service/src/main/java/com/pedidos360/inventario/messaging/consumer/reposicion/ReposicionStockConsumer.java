package com.pedidos360.inventario.messaging.consumer.reposicion;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.messaging.config.ConvertidorCrudoRabbitConfig;
import com.pedidos360.inventario.messaging.consumer.ConsumidorRabbit;
import com.pedidos360.inventario.messaging.consumer.ProcesadorMensajes;
import com.pedidos360.inventario.service.ReposicionStockService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Dominio reposición de stock: devuelve las unidades al inventario cuando se cancela un pedido. */
@Component
public class ReposicionStockConsumer extends ConsumidorRabbit {

    private final ReposicionStockService reposicionService;

    public ReposicionStockConsumer(ProcesadorMensajes procesador, ReposicionStockService reposicionService) {
        super(procesador);
        this.reposicionService = reposicionService;
    }

    @RabbitListener(queues = "${app.mensajeria.reposicion.cola}",
            messageConverter = ConvertidorCrudoRabbitConfig.NOMBRE_BEAN)
    public void alRecibirPedidoCancelado(Message mensaje, Channel canal) {
        consumir(mensaje, canal, PedidoEvento.class, reposicionService::reponer);
    }
}