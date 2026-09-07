package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.model.EventoAuditoria;
import com.pedidos360.pedidos.repository.EventoAuditoriaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/eventos-auditoria")
public class EventoAuditoriaController {

    private final EventoAuditoriaRepository eventoRepository;

    public EventoAuditoriaController(EventoAuditoriaRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @GetMapping
    public List<EventoAuditoria> listar() {
        return eventoRepository.findAllByOrderByFechaDesc();
    }
}
