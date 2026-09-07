package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.client.InventarioClient;
import com.pedidos360.pedidos.client.ProductoInventarioResponse;
import com.pedidos360.pedidos.dto.ItemRequest;
import com.pedidos360.pedidos.dto.PedidoRequest;
import com.pedidos360.pedidos.model.EventoAuditoria;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.model.PedidoItem;
import com.pedidos360.pedidos.repository.EventoAuditoriaRepository;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EventoAuditoriaRepository eventoRepository;
    private final InventarioClient inventarioClient;

    public PedidoService(PedidoRepository pedidoRepository,
                          EventoAuditoriaRepository eventoRepository,
                          InventarioClient inventarioClient) {
        this.pedidoRepository = pedidoRepository;
        this.eventoRepository = eventoRepository;
        this.inventarioClient = inventarioClient;
    }

    /**
     * NOTA para más adelante: esto llama a otro microservicio (reservarStock) DENTRO de una
     * transacción local. Si el pedido fallara al guardar después de reservar el stock, el
     * stock quedaría descontado sin un pedido asociado (los @Transactional de Spring no
     * "deshacen" llamadas HTTP a otro servicio). Para un proyecto de semestre esto es una
     * simplificación aceptable; la solución real de arquitectura de microservicios es un
     * patrón Saga con una acción compensatoria (devolver el stock) si el paso siguiente falla.
     */
    @Transactional
    public Pedido crearPedido(PedidoRequest request) {
        List<PedidoItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ItemRequest itemReq : request.items()) {
            ProductoInventarioResponse producto = inventarioClient.obtenerProducto(itemReq.productoId());
            inventarioClient.reservarStock(itemReq.productoId(), itemReq.cantidad());

            PedidoItem item = new PedidoItem();
            item.setProductoId(producto.id());
            item.setNombreProducto(producto.nombre());
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(producto.precio());
            items.add(item);

            total = total.add(producto.precio().multiply(BigDecimal.valueOf(itemReq.cantidad())));
        }

        Pedido pedido = new Pedido();
        pedido.setClienteNombre(request.clienteNombre());
        pedido.setClienteCorreo(request.clienteCorreo());
        pedido.setLocalId(request.localId());
        pedido.setModalidad(request.modalidad());
        pedido.setDireccionDespacho(request.direccionDespacho());
        pedido.setEstado("Pendiente");
        pedido.setTotal(total);

        items.forEach(item -> item.setPedido(pedido));
        pedido.setItems(items);

        Pedido guardado = pedidoRepository.save(pedido);
        registrarEvento(guardado.getId(), "Pedido #" + guardado.getId() + " creado por " + guardado.getClienteNombre() + ".", "cliente");
        return guardado;
    }

    public Pedido cambiarEstadoCocina(Long id, String nuevoEstado) {
        Pedido pedido = buscarOFallar(id);
        pedido.setEstado(nuevoEstado);
        Pedido guardado = pedidoRepository.save(pedido);
        registrarEvento(id, "Pedido #" + id + " pasó a " + nuevoEstado + ".", "cocina");
        return guardado;
    }

    public Pedido marcarEntregado(Long id) {
        Pedido pedido = buscarOFallar(id);
        pedido.setEstado("Entregado");
        Pedido guardado = pedidoRepository.save(pedido);
        registrarEvento(id, "Pedido #" + id + " entregado.", "despacho");
        return guardado;
    }

    public Pedido buscarOFallar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido " + id + " no existe"));
    }

    private void registrarEvento(Long pedidoId, String descripcion, String rol) {
        eventoRepository.save(new EventoAuditoria(pedidoId, descripcion, rol));
    }
}
