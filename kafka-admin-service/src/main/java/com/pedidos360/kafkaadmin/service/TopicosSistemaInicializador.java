package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.dto.TopicRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar, crea los tópicos del sistema (los definidos en application.yml) que todavía no existan.
 * Así un entorno nuevo queda listo solo, con las particiones, réplicas y configuración correctas.
 * Si Kafka aún no está disponible, reintenta unas veces y, si no logra conectar, solo deja un aviso.
 */
@Component
public class TopicosSistemaInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopicosSistemaInicializador.class);
    private static final int INTENTOS = 6;
    private static final long ESPERA_MS = 5_000;

    private final KafkaAdminService servicio;
    private final KafkaAdminProperties props;

    public TopicosSistemaInicializador(KafkaAdminService servicio, KafkaAdminProperties props) {
        this.servicio = servicio;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                asegurarTopicos();
                return;
            } catch (RuntimeException ex) {
                log.warn("Kafka no está listo para crear los tópicos del sistema (intento {}/{}): {}",
                        intento, INTENTOS, ex.getMessage());
                if (intento == INTENTOS || !esperar()) {
                    log.warn("No se pudieron crear los tópicos del sistema al arrancar; pueden crearse luego con la API");
                    return;
                }
            }
        }
    }

    private void asegurarTopicos() {
        List<String> existentes = servicio.listarTopicos();
        for (KafkaAdminProperties.TopicoSistema topico : props.topicosSistema()) {
            if (existentes.contains(topico.nombre())) {
                log.info("Tópico del sistema '{}' ya existe", topico.nombre());
                continue;
            }
            servicio.crearTopico(new TopicRequest(topico.nombre(), topico.particiones(),
                    topico.replicacion(), topico.configuracion()));
        }
    }

    private boolean esperar() {
        try {
            Thread.sleep(ESPERA_MS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
