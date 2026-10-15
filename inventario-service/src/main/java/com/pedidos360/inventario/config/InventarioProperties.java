package com.pedidos360.inventario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de negocio de inventario configurables en application.yml (prefijo "app.inventario").
 *
 * @param umbralStockBajo un producto tiene poco stock cuando le quedan menos unidades que este valor
 */
@ConfigurationProperties(prefix = "app.inventario")
public record InventarioProperties(int umbralStockBajo) {
}