package com.minewaku.chatter.message.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication(scanBasePackages = {"com.minewaku.chatter.message"})
@EnableJdbcRepositories(basePackages = "com.minewaku.chatter.message.infrastructure.persistence.postgresql")
@EnableCassandraRepositories(basePackages = "com.minewaku.chatter.message.infrastructure.persistence.scylladb")
@EntityScan(basePackages = "com.minewaku.chatter.message.infrastructure.persistence.postgresql.entity")
@EnableDiscoveryClient
public class StartApplication {
	public static void main(String[] args) {
		SpringApplication.run(StartApplication.class, args);
	}
}
