package com.pedidos360.kafkaadmin;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class KafkaAdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaAdminServiceApplication.class, args);
	}

}
