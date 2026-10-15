package com.pedidos360.inventario.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa la lectura de las reglas de inventario desde application.yml. */
@Configuration
@EnableConfigurationProperties(InventarioProperties.class)
public class InventarioPropertiesConfig {
}