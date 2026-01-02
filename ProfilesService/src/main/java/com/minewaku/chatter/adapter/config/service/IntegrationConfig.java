package com.minewaku.chatter.adapter.config.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class IntegrationConfig {
	
	@Bean
    @Qualifier("outboxChannel")
    public MessageChannel outboxChannel() {
        return new QueueChannel();
    }
}
