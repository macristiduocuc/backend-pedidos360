package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.NotificacionesResponse;
import com.pedidos360.pedidos.service.Destinatario;
import com.pedidos360.pedidos.service.NotificacionConsultaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Campanita del front. El usuario nunca indica de quién quiere ver las notificaciones:
 * se deduce de su token (cliente: su correo; auditoría: las alertas de su rol).
 */
@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("hasAnyRole('CLIENTE', 'AUDITORIA')")
public class NotificacionController {

    private final NotificacionConsultaService servicio;

    public NotificacionController(NotificacionConsultaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mias")
    public NotificacionesResponse mias(Authentication authentication) {
        return servicio.listar(Destinatario.de(authentication));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, Authentication authentication) {
        boolean marcada = servicio.marcarLeida(id, Destinatario.de(authentication));
        return marcada ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/leidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasLeidas(Authentication authentication) {
        servicio.marcarTodasLeidas(Destinatario.de(authentication));
    }
}