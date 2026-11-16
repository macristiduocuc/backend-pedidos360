package com.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/** Cliente de administración de Kafka: es la puerta de este servicio hacia los brokers. */
@Configuration
public class KafkaClientConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                            KafkaAdminProperties props) {
        int timeoutMs = props.timeoutSegundos() * 1000;
        return Admin.create(Map.<String, Object>of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMs,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, timeoutMs));
    }
}
