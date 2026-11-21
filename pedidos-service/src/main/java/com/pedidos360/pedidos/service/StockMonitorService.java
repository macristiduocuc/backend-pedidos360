package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.StockActualResponse;
import com.pedidos360.pedidos.event.StockActualizadoEvento;
import com.pedidos360.pedidos.kafka.consumer.EventoInvalidoException;
import com.pedidos360.pedidos.repository.StockActualRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Mantiene el último stock conocido de cada producto a partir del tópico inventario.stock.
 * Es idempotente por naturaleza: cada mensaje reemplaza el valor anterior del mismo producto,
 * así que procesar un mensaje repetido no cambia nada.
 */
@Service
public class StockMonitorService {

    private static final Logger log = LoggerFactory.getLogger(StockMonitorService.class);

    private final StockActualRepository repositorio;

    public StockMonitorService(StockActualRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public void registrar(ConsumerRecord<String, StockActualizadoEvento> registro) {
        StockActualizadoEvento evento = registro.value();
        if (evento == null || evento.productoId() == null || evento.stock() == null || evento.stock() < 0) {
            throw new EventoInvalidoException("El mensaje no trae el producto o un stock válido");
        }
        OffsetDateTime momento = evento.ocurridoEn() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : OffsetDateTime.ofInstant(evento.ocurridoEn(), ZoneOffset.UTC);

        repositorio.guardar(evento.productoId(), evento.nombreProducto(), evento.localId(), evento.stock(), momento);
        log.info("Stock del producto {} actualizado a {} (partición {}, offset {})",
                evento.productoId(), evento.stock(), registro.partition(), registro.offset());
    }

    @Transactional(readOnly = true)
    public List<StockActualResponse> listar() {
        return repositorio.findTop100ByOrderByStockAscNombreProductoAsc().stream()
                .map(s -> new StockActualResponse(s.getProductoId(), s.getNombreProducto(), s.getLocalId(),
                        s.getStock(), s.getOcurridoEn(), s.getActualizadoEn()))
                .toList();
    }
}