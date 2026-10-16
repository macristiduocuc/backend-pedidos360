package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.ExchangeRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeResponse;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges: POST /api/exchanges, DELETE /api/exchanges/{nombre}. Solo rol AUDITORIA. */
@RestController
@RequestMapping("/api/exchanges")
@PreAuthorize("hasRole('AUDITORIA')")
public class ExchangeController {

    private final RabbitAdminService servicio;

    public ExchangeController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeResponse crear(@Valid @RequestBody ExchangeRequest solicitud) {
        return servicio.crearExchange(solicitud);
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) {
        servicio.eliminarExchange(nombre);
    }
}
