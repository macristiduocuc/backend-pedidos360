package com.pedidos360.inventario.repository;

import com.pedidos360.inventario.dto.ProductoVendidoResponse;
import com.pedidos360.inventario.model.VentaProducto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    /** Suma (o resta, si es negativo) unidades de forma atómica; crea la fila si el producto aún no existe. */
    @Modifying
    @Query(value = "INSERT INTO ventas_por_producto (producto_id, unidades_vendidas) VALUES (:productoId, :unidades) "
            + "ON CONFLICT (producto_id) DO UPDATE SET "
            + "unidades_vendidas = ventas_por_producto.unidades_vendidas + :unidades, actualizado_en = now()",
            nativeQuery = true)
    void sumarUnidades(@Param("productoId") Long productoId, @Param("unidades") long unidades);

    @Query("SELECT new com.pedidos360.inventario.dto.ProductoVendidoResponse(v.productoId, p.nombre, v.unidadesVendidas) "
            + "FROM VentaProducto v, Producto p WHERE p.id = v.productoId ORDER BY v.unidadesVendidas DESC")
    List<ProductoVendidoResponse> masVendidos(Pageable pagina);
}