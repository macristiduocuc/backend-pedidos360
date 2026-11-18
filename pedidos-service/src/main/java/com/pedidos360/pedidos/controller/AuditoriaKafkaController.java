package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.AuditoriaKafkaResponse;
import com.pedidos360.pedidos.service.AuditoriaKafkaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Historial de eventos guardado desde Kafka. Solo rol AUDITORIA.
 * GET /api/auditoria-kafka            -> los últimos 100 eventos
 * GET /api/auditoria-kafka?pedidoId=N -> los eventos de un pedido, en el orden en que ocurrieron
 */
@RestController
@RequestMapping("/api/auditoria-kafka")
@PreAuthorize("hasRole('AUDITORIA')")
public class AuditoriaKafkaController {

    private final AuditoriaKafkaService servicio;

    public AuditoriaKafkaController(AuditoriaKafkaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<AuditoriaKafkaResponse> listar(@RequestParam(required = false) Long pedidoId) {
        return servicio.listar(pedidoId);
    }
}