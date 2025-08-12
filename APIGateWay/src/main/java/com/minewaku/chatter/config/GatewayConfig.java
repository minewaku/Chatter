package com.minewaku.chatter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RateLimitProperties.class})
public class GatewayConfig {
    // Additional gateway beans can be configured here
}