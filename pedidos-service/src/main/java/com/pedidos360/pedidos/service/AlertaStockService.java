package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.event.AlertaStockEvento;
import com.pedidos360.pedidos.messaging.consumer.MensajeNoProcesableException;
import com.pedidos360.pedidos.model.Notificacion;
import com.pedidos360.pedidos.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Convierte una alerta de stock bajo en una notificación para el rol Auditoría. */
@Service
public class AlertaStockService {

    private static final Logger log = LoggerFactory.getLogger(AlertaStockService.class);
    private static final String ROL_DESTINO = "AUDITORIA";

    private final NotificacionRepository notificacionRepository;

    public AlertaStockService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional
    public void registrarAlerta(AlertaStockEvento evento) {
        if (evento == null || evento.productoId() == null || evento.stockActual() == null) {
            throw new MensajeNoProcesableException("La alerta no trae el producto o el stock actual");
        }

        String producto = evento.nombreProducto() != null ? evento.nombreProducto() : "producto #" + evento.productoId();
        String texto = "Poco stock de " + producto + " (local " + evento.localId() + "): quedan "
                + evento.stockActual() + (evento.stockMinimo() != null ? ", el mínimo es " + evento.stockMinimo() : "") + ".";

        notificacionRepository.save(Notificacion.paraRol(null, ROL_DESTINO, texto));
        log.info("Alerta de stock bajo registrada para {} del producto {}", ROL_DESTINO, evento.productoId());
    }
}