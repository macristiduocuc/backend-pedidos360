package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.NotificacionResponse;
import com.pedidos360.pedidos.dto.NotificacionesResponse;
import com.pedidos360.pedidos.model.Notificacion;
import com.pedidos360.pedidos.repository.NotificacionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lo que necesita la campanita del front: listar las notificaciones de quien consulta y marcarlas
 * como leídas. Solo toca las notificaciones de ese destinatario; una notificación ajena
 * se trata como si no existiera.
 */
@Service
public class NotificacionConsultaService {

    private static final int MAXIMO_EN_LISTA = 30;

    private final NotificacionRepository repositorio;

    public NotificacionConsultaService(NotificacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public NotificacionesResponse listar(Destinatario destinatario) {
        Pageable pagina = PageRequest.of(0, MAXIMO_EN_LISTA);
        List<Notificacion> recientes;
        long noLeidas;
        if (destinatario.esDeRol()) {
            recientes = repositorio.findByDestinatarioRolOrderByIdDesc(destinatario.rol(), pagina);
            noLeidas = repositorio.countByDestinatarioRolAndLeidaFalse(destinatario.rol());
        } else {
            recientes = repositorio.findByDestinatarioCorreoIgnoreCaseOrderByIdDesc(destinatario.correo(), pagina);
            noLeidas = repositorio.countByDestinatarioCorreoIgnoreCaseAndLeidaFalse(destinatario.correo());
        }
        return new NotificacionesResponse(noLeidas, recientes.stream().map(this::aRespuesta).toList());
    }

    /** Devuelve false si la notificación no existe o no es de este destinatario. */
    @Transactional
    public boolean marcarLeida(Long id, Destinatario destinatario) {
        Optional<Notificacion> notificacion = destinatario.esDeRol()
                ? repositorio.findByIdAndDestinatarioRol(id, destinatario.rol())
                : repositorio.findByIdAndDestinatarioCorreoIgnoreCase(id, destinatario.correo());
        notificacion.ifPresent(n -> {
            n.setLeida(true);
            repositorio.save(n);
        });
        return notificacion.isPresent();
    }

    @Transactional
    public void marcarTodasLeidas(Destinatario destinatario) {
        List<Notificacion> pendientes = destinatario.esDeRol()
                ? repositorio.findByDestinatarioRolAndLeidaFalse(destinatario.rol())
                : repositorio.findByDestinatarioCorreoIgnoreCaseAndLeidaFalse(destinatario.correo());
        pendientes.forEach(n -> n.setLeida(true));
        repositorio.saveAll(pendientes);
    }

    private NotificacionResponse aRespuesta(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getPedidoId(), n.getMensaje(), n.isLeida(), n.getCreadoEn());
    }
}