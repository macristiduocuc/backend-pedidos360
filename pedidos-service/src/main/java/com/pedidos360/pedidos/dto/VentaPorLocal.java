package com.pedidos360.pedidos.dto;

import java.math.BigDecimal;

public record VentaPorLocal(Long localId, BigDecimal total) {}
