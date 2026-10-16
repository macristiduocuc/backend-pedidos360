package com.pedidos360.rabbitadmin.dto;

/** Tipos de exchange que se pueden crear. */
public enum TipoExchange {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS
}
