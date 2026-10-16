package com.pedidos360.rabbitadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Configuración del administrador (application.yml, prefijo "app.rabbit-admin"). */
@ConfigurationProperties(prefix = "app.rabbit-admin")
public record RabbitAdminProperties(Protegidos protegidos) {

    public RabbitAdminProperties {
        if (protegidos == null) {
            protegidos = new Protegidos(List.of(), List.of());
        }
    }

    /** Nombres de exchanges y colas que no se pueden eliminar desde la API. */
    public record Protegidos(List<String> exchanges, List<String> colas) {

        public Protegidos {
            exchanges = exchanges == null ? List.of() : List.copyOf(exchanges);
            colas = colas == null ? List.of() : List.copyOf(colas);
        }
    }
}
