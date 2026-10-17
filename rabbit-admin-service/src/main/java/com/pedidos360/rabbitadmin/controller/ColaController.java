package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.ColaRequest;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Colas: POST /api/queues, GET /api/queues/{nombre}, DELETE /api/queues/{nombre}. Solo rol AUDITORIA. */
@RestController
@RequestMapping("/api/queues")
@PreAuthorize("hasRole('AUDITORIA')")
public class ColaController {

    private final RabbitAdminService servicio;

    public ColaController(RabbitAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ColaResponse crear(@Valid @RequestBody ColaRequest solicitud) {
        return servicio.crearCola(solicitud);
    }

    @GetMapping("/{nombre}")
    public ColaResponse obtener(@PathVariable String nombre) {
        return servicio.obtenerCola(nombre);
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) {
        servicio.eliminarCola(nombre);
    }
}
