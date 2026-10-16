package com.pedidos360.rabbitadmin.dto;

public record ExchangeResponse(String nombre, TipoExchange tipo, boolean durable) {
}
