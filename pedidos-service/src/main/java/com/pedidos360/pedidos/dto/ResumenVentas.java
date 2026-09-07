package com.pedidos360.pedidos.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenVentas(
        BigDecimal ventasTotales,
        long totalPedidos,
        long totalEntregados,
        List<VentaPorLocal> porLocal
) {}
