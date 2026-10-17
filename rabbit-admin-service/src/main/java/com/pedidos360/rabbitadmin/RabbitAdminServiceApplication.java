package com.pedidos360.rabbitadmin;

import com.pedidos360.rabbitadmin.config.RabbitAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RabbitAdminProperties.class)
public class RabbitAdminServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RabbitAdminServiceApplication.class, args);
	}

}
