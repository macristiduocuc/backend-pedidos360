package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.ConsumerGroupResponse;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consumer groups: GET /api/consumer-groups y GET /api/consumer-groups/{id}. Solo rol AUDITORIA. */
@RestController
@RequestMapping("/api/consumer-groups")
@PreAuthorize("hasRole('AUDITORIA')")
public class ConsumerGroupController {

    private final KafkaAdminService servicio;

    public ConsumerGroupController(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<String> listar() {
        return servicio.listarGrupos();
    }

    @GetMapping("/{id}")
    public ConsumerGroupResponse obtener(@PathVariable String id) {
        return servicio.obtenerGrupo(id);
    }
}
