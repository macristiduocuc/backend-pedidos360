package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.StockActual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface StockActualRepository extends JpaRepository<StockActual, Long> {

    /**
     * Guarda el stock de un producto: inserta la fila o la actualiza. Si llega un mensaje más antiguo que el
     * ya guardado, no lo reemplaza.
     */
    @Modifying
    @Query(value = "INSERT INTO stock_actual (producto_id, nombre_producto, local_id, stock, ocurrido_en) "
            + "VALUES (:productoId, :nombre, :localId, :stock, :ocurridoEn) "
            + "ON CONFLICT (producto_id) DO UPDATE SET "
            + "nombre_producto = :nombre, local_id = :localId, stock = :stock, ocurrido_en = :ocurridoEn, "
            + "actualizado_en = now() "
            + "WHERE stock_actual.ocurrido_en IS NULL OR stock_actual.ocurrido_en <= :ocurridoEn",
            nativeQuery = true)
    void guardar(@Param("productoId") Long productoId,
                 @Param("nombre") String nombre,
                 @Param("localId") Long localId,
                 @Param("stock") int stock,
                 @Param("ocurridoEn") OffsetDateTime ocurridoEn);

    List<StockActual> findTop100ByOrderByStockAscNombreProductoAsc();
}