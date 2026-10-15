package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
}