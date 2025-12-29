package com.minewaku.chatter.adapter.config.service;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;

import com.minewaku.chatter.adapter.config.properties.VaultRedisProperties;
import com.minewaku.chatter.adapter.config.properties.VaultMailProperties;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties dsp = new DataSourceProperties();
        return dsp;
    }

    @Bean
    @Primary
    RedisProperties redisProperties(VaultRedisProperties vaultRedisProperties) {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setUsername(vaultRedisProperties.getUsername());
        redisProperties.setPassword(vaultRedisProperties.getPassword());

        return redisProperties;
    }

    @Bean
    @Primary
    MailProperties mailProperties(VaultMailProperties vaultMailProperties) {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername(vaultMailProperties.getUsername());
        mailProperties.setPassword(vaultMailProperties.getPassword());
        return mailProperties;
    }

    @Bean
    @Primary
    JdbcTemplate jdbcTemplate(DataSourceProperties dataSourceProperties) {
        return new JdbcTemplate(dataSourceProperties.initializeDataSourceBuilder().build());
    }

    @Bean
    SpelExpressionParser spelExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }
}
