package com.minewaku.chatter.identityaccess.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication(scanBasePackages = {"com.minewaku.chatter.identityaccess"})
@EnableJdbcRepositories(basePackages = "com.minewaku.chatter.identityaccess.infrastructure.persistence")
@EntityScan(basePackages = "com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.entity")
@EnableDiscoveryClient
public class StartApplication {
	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}
}
