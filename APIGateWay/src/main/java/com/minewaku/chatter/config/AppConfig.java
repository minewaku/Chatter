package com.minewaku.chatter.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.minewaku.chatter.config.properties.CacheProperties;

@Configuration
public class AppConfig {
 
    @Bean
    @Primary
    RedisProperties redisProperties(CacheProperties cacheProperties) {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUsername(cacheProperties.getUsername());
        redisProperties.setPassword(cacheProperties.getPassword());

        return redisProperties;
    }
}
