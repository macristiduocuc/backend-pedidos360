package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioCorreoIgnoreCaseOrderByIdDesc(String correo, Pageable pagina);

    List<Notificacion> findByDestinatarioRolOrderByIdDesc(String rol, Pageable pagina);

    long countByDestinatarioCorreoIgnoreCaseAndLeidaFalse(String correo);

    long countByDestinatarioRolAndLeidaFalse(String rol);

    Optional<Notificacion> findByIdAndDestinatarioCorreoIgnoreCase(Long id, String correo);

    Optional<Notificacion> findByIdAndDestinatarioRol(Long id, String rol);

    List<Notificacion> findByDestinatarioCorreoIgnoreCaseAndLeidaFalse(String correo);

    List<Notificacion> findByDestinatarioRolAndLeidaFalse(String rol);
}