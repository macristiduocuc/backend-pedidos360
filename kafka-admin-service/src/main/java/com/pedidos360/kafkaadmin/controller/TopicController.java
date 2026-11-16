package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.ParticionesRequest;
import com.pedidos360.kafkaadmin.dto.TopicConfigRequest;
import com.pedidos360.kafkaadmin.dto.TopicRequest;
import com.pedidos360.kafkaadmin.dto.TopicResponse;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tópicos: POST /api/topics, GET /api/topics, GET /api/topics/{nombre},
 * PATCH /api/topics/{nombre}/config, PATCH /api/topics/{nombre}/partitions y DELETE /api/topics/{nombre}.
 * Solo rol AUDITORIA.
 */
@RestController
@RequestMapping("/api/topics")
@PreAuthorize("hasRole('AUDITORIA')")
public class TopicController {

    private final KafkaAdminService servicio;

    public TopicController(KafkaAdminService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse crear(@Valid @RequestBody TopicRequest solicitud) {
        return servicio.crearTopico(solicitud);
    }

    @GetMapping
    public List<String> listar() {
        return servicio.listarTopicos();
    }

    @GetMapping("/{nombre}")
    public TopicResponse obtener(@PathVariable String nombre) {
        return servicio.obtenerTopico(nombre);
    }

    @PatchMapping("/{nombre}/config")
    public TopicResponse modificarConfiguracion(@PathVariable String nombre,
                                                @Valid @RequestBody TopicConfigRequest solicitud) {
        return servicio.modificarConfiguracion(nombre, solicitud.configuracion());
    }

    @PatchMapping("/{nombre}/partitions")
    public TopicResponse aumentarParticiones(@PathVariable String nombre,
                                             @Valid @RequestBody ParticionesRequest solicitud) {
        return servicio.aumentarParticiones(nombre, solicitud.total());
    }

    @DeleteMapping("/{nombre}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombre) {
        servicio.eliminarTopico(nombre);
    }
}
