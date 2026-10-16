package com.pedidos360.rabbitadmin.dto;

/** Estado de una cola: cuántos mensajes tiene y cuántos consumidores la escuchan. */
public record ColaResponse(String nombre, long mensajes, long consumidores) {
}