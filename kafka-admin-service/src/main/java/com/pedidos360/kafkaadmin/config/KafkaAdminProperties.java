package com.pedidos360.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Configuración del administrador de Kafka (application.yml, prefijo "app.kafka-admin"). */
@ConfigurationProperties(prefix = "app.kafka-admin")
public record KafkaAdminProperties(int timeoutSegundos, List<TopicoSistema> topicosSistema) {

    public KafkaAdminProperties {
        if (timeoutSegundos < 1) {
            timeoutSegundos = 10;
        }
        topicosSistema = topicosSistema == null ? List.of() : List.copyOf(topicosSistema);
    }

    /** Tópico que forma parte del sistema: se crea al arrancar y no se puede eliminar. */
    public record TopicoSistema(String nombre, int particiones, int replicacion, Map<String, String> configuracion) {

        public TopicoSistema {
            configuracion = configuracion == null ? Map.of() : Map.copyOf(configuracion);
        }
    }

    /** Nombres de los tópicos protegidos. */
    public Set<String> nombresProtegidos() {
        return topicosSistema.stream().map(TopicoSistema::nombre).collect(Collectors.toSet());
    }
}
