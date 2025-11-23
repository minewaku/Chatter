package com.minewaku.chatter.adapter.config.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;

@Component
public class CerbosConfig {
    @Bean
    CerbosBlockingClient cerbosBlockingClient() {
        try {
            CerbosBlockingClient client = new CerbosClientBuilder("localhost:3593")
                .withPlaintext()
                .buildBlockingClient();
            return client;
        } catch(Exception e) {
            throw new RuntimeException("Failed to create CerbosBlockingClient", e);
        }
    }
}
