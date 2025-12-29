package com.minewaku.chatter.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.minewaku.chatter.config.properties.VaultRedisProperties;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    RedisProperties redisProperties(VaultRedisProperties vaultRedisProperties) {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUsername(vaultRedisProperties.getUsername());
        redisProperties.setPassword(vaultRedisProperties.getPassword());

        return redisProperties;
    }
}
