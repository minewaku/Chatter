package com.minewaku.chatter.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.config.properties.CerbosProperties;

import dev.cerbos.sdk.CerbosBlockingAdminClient;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;

@Component
public class CerbosConfig {

    @Bean
    CerbosBlockingClient cerbosBlockingClient() {
        try {
            CerbosBlockingClient client = new CerbosClientBuilder("localhost:5007")
                .withPlaintext()
                .withTimeout(Duration.ofSeconds(5))
                .buildBlockingClient();
            return client;
        } catch(Exception e) {
            throw new RuntimeException("Failed to create CerbosBlockingClient", e);
        }
    }

    @Bean
    CerbosBlockingAdminClient cerbosBlockingAdminClient(CerbosProperties CerbosProperties) {
        try {
            CerbosBlockingAdminClient adminClient = new CerbosClientBuilder("localhost:5007")
                .withPlaintext()
                .withTimeout(Duration.ofSeconds(5))
                .buildBlockingAdminClient(CerbosProperties.getUsername(), CerbosProperties.getPassword());
            return adminClient;
        } catch(Exception e) {
            throw new RuntimeException("Failed to create CerbosBlockingAdminClient", e);
        }
    }
}

