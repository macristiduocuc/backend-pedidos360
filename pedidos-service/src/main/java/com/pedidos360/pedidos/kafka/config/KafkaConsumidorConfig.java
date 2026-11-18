package com.pedidos360.pedidos.kafka.config;

import com.pedidos360.pedidos.kafka.consumer.EventoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Manejo de errores de todos los consumidores de Kafka:
 *  - Mensaje ilegible o dato inválido (EventoInvalidoException): NO se reintenta; se registra y se sigue.
 *  - Cualquier otro error (por ejemplo, la base de datos no responde): se reintenta hasta max-intentos
 *    y, si sigue fallando, se registra en el log y se continúa con el siguiente mensaje.
 * Así un mensaje malo nunca deja detenido al consumidor.
 */
@Configuration
@EnableConfigurationProperties({KafkaGruposProperties.class, KafkaConsumidorProperties.class})
public class KafkaConsumidorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumidorConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaConsumidorProperties props) {
        ConsumerRecordRecoverer descartar = (registro, error) -> log.error(
                "Evento descartado (tópico {}, partición {}, offset {}): {}",
                registro.topic(), registro.partition(), registro.offset(), mensajeDe(error));

        FixedBackOff espera = new FixedBackOff(props.esperaReintento().toMillis(), props.maxIntentos() - 1L);
        DefaultErrorHandler manejador = new DefaultErrorHandler(descartar, espera);
        manejador.addNotRetryableExceptions(EventoInvalidoException.class);
        return manejador;
    }

    private static String mensajeDe(Throwable error) {
        Throwable causa = error.getCause() != null ? error.getCause() : error;
        return causa.getMessage() != null ? causa.getMessage() : causa.toString();
    }
}