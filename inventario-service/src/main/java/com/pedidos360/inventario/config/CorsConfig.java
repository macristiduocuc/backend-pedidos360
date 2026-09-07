package com.pedidos360.inventario.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Habilitamos el origen local del frontend. Cuando despleguemos, agregamos
        // también el dominio público (https://pedidos360.duckdns.org) a esta lista.
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "https://pedidos360.duckdns.org")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
