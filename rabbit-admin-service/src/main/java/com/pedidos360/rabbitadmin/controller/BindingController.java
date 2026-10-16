package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.BindingResponse;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bindings: POST /api/bindings y DELETE /api/bindings?exchange=...&cola=...&routingKey=...
 * (un binding no tiene id propio: se identifica por exchange, cola y routing key). Solo rol AUDITORIA.
 */
@RestController
@RequestMapping("/api/bindings")
@PreAuthorize("hasRole('AUDITORIA')")
public class BindingController {

    private final RabbitAdminService servicio;

    public BindingController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingResponse crear(@Valid @RequestBody BindingRequest solicitud) {
        return servicio.crearBinding(solicitud);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestParam String exchange,
                         @RequestParam String cola,
                         @RequestParam String routingKey) {
        servicio.eliminarBinding(exchange, cola, routingKey);
    }
}
