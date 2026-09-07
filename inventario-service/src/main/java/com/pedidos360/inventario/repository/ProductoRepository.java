package com.pedidos360.inventario.repository;

import com.pedidos360.inventario.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrue();

    List<Producto> findByActivoTrueAndCategoria_NombreIgnoreCase(String nombreCategoria);

    /**
     * UPDATE atómico: el WHERE p.stock >= :cantidad hace que la propia base de datos
     * sea quien decide, fila por fila, si hay stock suficiente. Bajo dos compras
     * simultáneas del último producto, solo una de las dos consultas afecta una fila;
     * la otra devuelve 0 y el servicio la rechaza. No hace falta lock manual.
     */
    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int reservarStock(@Param("id") Long id, @Param("cantidad") Integer cantidad);
}
