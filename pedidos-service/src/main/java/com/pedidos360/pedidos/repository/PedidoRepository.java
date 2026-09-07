package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.dto.VentaPorLocal;
import com.pedidos360.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByEstadoOrderByCreadoEnAsc(String estado);

    List<Pedido> findByClienteCorreoOrderByCreadoEnDesc(String clienteCorreo);

    List<Pedido> findAllByOrderByCreadoEnDesc();

    long countByEstado(String estado);

    @Query("""
            SELECT new com.pedidos360.pedidos.dto.VentaPorLocal(p.localId, SUM(p.total))
            FROM Pedido p
            WHERE p.estado <> 'Cancelado'
            GROUP BY p.localId
            ORDER BY SUM(p.total) DESC
            """)
    List<VentaPorLocal> ventasPorLocal();
}
