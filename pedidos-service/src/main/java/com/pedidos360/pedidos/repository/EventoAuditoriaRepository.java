package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {

    List<EventoAuditoria> findAllByOrderByFechaDesc();
}
