package com.pedidos360.inventario.repository;

import com.pedidos360.inventario.model.VentaRegistrada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRegistradaRepository extends JpaRepository<VentaRegistrada, Long> {
}