package com.minewaku.chatter.profile.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.minewaku.chatter.profile"})
@EnableJpaRepositories(basePackages = "com.minewaku.chatter.profile.infrastructure.persistence")
@EntityScan(basePackages = "com.minewaku.chatter.profile.domain.model")
@EnableDiscoveryClient
public class StartApplication {
	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}
}
