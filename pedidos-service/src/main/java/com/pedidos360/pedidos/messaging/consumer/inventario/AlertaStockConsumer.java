package com.pedidos360.pedidos.messaging.consumer.inventario;

import com.pedidos360.pedidos.event.AlertaStockEvento;
import com.pedidos360.pedidos.messaging.config.ConvertidorCrudoRabbitConfig;
import com.pedidos360.pedidos.messaging.consumer.ConsumidorRabbit;
import com.pedidos360.pedidos.messaging.consumer.ProcesadorMensajes;
import com.pedidos360.pedidos.service.AlertaStockService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Dominio inventario: recibe las alertas de stock bajo que publica inventario-service. */
@Component
public class AlertaStockConsumer extends ConsumidorRabbit {

    private final AlertaStockService alertaStockService;

    public AlertaStockConsumer(ProcesadorMensajes procesador, AlertaStockService alertaStockService) {
        super(procesador);
        this.alertaStockService = alertaStockService;
    }

    @RabbitListener(queues = "${app.mensajeria.alerta-stock.cola}",
            messageConverter = ConvertidorCrudoRabbitConfig.NOMBRE_BEAN)
    public void alRecibirAlertaDeStock(Message mensaje, Channel canal) {
        consumir(mensaje, canal, AlertaStockEvento.class, alertaStockService::registrarAlerta);
    }
}