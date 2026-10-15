package com.minewaku.chatter.identityaccess.infrastructure.integration.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Connection-level Redis setup shared by the verification and session adapters. Connection settings come from
 * {@code spring.data.redis.*}; all stored values are strings so the adapters control their own encoding.
 */
@Configuration
public class RedisConfiguration {

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
