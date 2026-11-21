package com.pedidos360.inventario.controller;

import com.pedidos360.inventario.dto.ProductoVendidoResponse;
import com.pedidos360.inventario.service.AnaliticaVentasService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Analítica de ventas calculada desde Kafka. Solo rol AUDITORIA. */
@RestController
@RequestMapping("/api/analitica")
@PreAuthorize("hasRole('AUDITORIA')")
public class AnaliticaController {

    private final AnaliticaVentasService servicio;

    public AnaliticaController(AnaliticaVentasService servicio) {
        this.servicio = servicio;
    }

    /** Los productos más vendidos (por defecto 10, máximo 50). */
    @GetMapping("/productos-mas-vendidos")
    public List<ProductoVendidoResponse> masVendidos(@RequestParam(defaultValue = "10") int limite) {
        return servicio.productosMasVendidos(limite);
    }
}