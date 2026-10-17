package com.pedidos360.inventario.messaging.consumer;

import com.pedidos360.inventario.event.PedidoEvento;
import com.pedidos360.inventario.messaging.consumer.reposicion.ReposicionStockConsumer;
import com.pedidos360.inventario.service.ReposicionStockService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Comprueba que el consumidor confirma (ack) o rechaza (nack) cada mensaje según el resultado. */
class ConsumidorAckTest {

    private final ProcesadorMensajes procesador = mock(ProcesadorMensajes.class);
    private final ReposicionStockService reposicionService = mock(ReposicionStockService.class);
    private final Channel canal = mock(Channel.class);
    private final ReposicionStockConsumer consumidor = new ReposicionStockConsumer(procesador, reposicionService);

    private Message mensaje() {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(9L);
        return new Message(new byte[0], propiedades);
    }

    private void simularResultado(ResultadoProceso resultado) {
        when(procesador.procesar(any(Message.class), eq(PedidoEvento.class), any())).thenReturn(resultado);
    }

    @Test
    void siSeProcesoBienHaceAck() throws Exception {
        simularResultado(ResultadoProceso.EXITO);

        consumidor.alRecibirPedidoCancelado(mensaje(), canal);

        verify(canal).basicAck(9L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void siSeDescartaHaceNackSinReencolarParaQueVayaALaDlq() throws Exception {
        simularResultado(ResultadoProceso.DESCARTADO);

        consumidor.alRecibirPedidoCancelado(mensaje(), canal);

        verify(canal).basicNack(9L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void siElProcesadorFallaDeFormaInesperadaIgualHaceNackSinReencolar() throws Exception {
        when(procesador.procesar(any(Message.class), eq(PedidoEvento.class), any()))
                .thenThrow(new IllegalStateException("fallo inesperado"));

        consumidor.alRecibirPedidoCancelado(mensaje(), canal);

        verify(canal).basicNack(9L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void siSeInterrumpeHaceNackReencolando() throws Exception {
        simularResultado(ResultadoProceso.REENCOLAR);

        consumidor.alRecibirPedidoCancelado(mensaje(), canal);

        verify(canal).basicNack(9L, false, true);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }
}