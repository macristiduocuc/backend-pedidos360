package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.StockActualResponse;
import com.pedidos360.pedidos.service.StockMonitorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Último stock de cada producto, recibido desde Kafka (primero los de menos stock). Solo rol AUDITORIA. */
@RestController
@RequestMapping("/api/stock-actual")
@PreAuthorize("hasRole('AUDITORIA')")
public class StockActualController {

    private final StockMonitorService servicio;

    public StockActualController(StockMonitorService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<StockActualResponse> listar() {
        return servicio.listar();
    }
}