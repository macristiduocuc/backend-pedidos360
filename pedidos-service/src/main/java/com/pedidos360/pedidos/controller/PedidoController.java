package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.dto.CambiarEstadoRequest;
import com.pedidos360.pedidos.dto.PedidoRequest;
import com.pedidos360.pedidos.dto.PedidoResponse;
import com.pedidos360.pedidos.dto.ResumenVentas;
import com.pedidos360.pedidos.repository.PedidoRepository;
import com.pedidos360.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;

    public PedidoController(PedidoService pedidoService, PedidoRepository pedidoRepository) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) String estado) {
        var pedidos = (estado == null || estado.isBlank())
                ? pedidoRepository.findAllByOrderByCreadoEnDesc()
                : pedidoRepository.findByEstadoOrderByCreadoEnAsc(estado);
        return pedidos.stream().map(PedidoResponse::desde).toList();
    }

    @GetMapping("/cliente/{correo}")
    public List<PedidoResponse> misPedidos(@PathVariable String correo) {
        return pedidoRepository.findByClienteCorreoOrderByCreadoEnDesc(correo)
                .stream().map(PedidoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return PedidoResponse.desde(pedidoService.buscarOFallar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request) {
        return PedidoResponse.desde(pedidoService.crearPedido(request));
    }

    /** Usado por la pantalla de Cocina: Pendiente -> En preparación -> Hecho. */
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return PedidoResponse.desde(pedidoService.cambiarEstadoCocina(id, request.nuevoEstado()));
    }

    /** Usado por la pantalla de Despacho. */
    @PatchMapping("/{id}/entregar")
    public PedidoResponse entregar(@PathVariable Long id) {
        return PedidoResponse.desde(pedidoService.marcarEntregado(id));
    }
    /** Para el dashboard de Auditoría: totales generales + ventas agrupadas por local. */
    @GetMapping("/resumen")
    public ResumenVentas resumen() {
        var porLocal = pedidoRepository.ventasPorLocal();
        BigDecimal ventasTotales = porLocal.stream().map(v -> v.total()).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalPedidos = pedidoRepository.count();
        long totalEntregados = pedidoRepository.countByEstado("Entregado");
        return new ResumenVentas(ventasTotales, totalPedidos, totalEntregados, porLocal);
    }
}
