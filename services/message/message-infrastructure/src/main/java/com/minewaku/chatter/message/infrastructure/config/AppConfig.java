package com.minewaku.chatter.message.infrastructure.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.NoArgGenerator;


@Configuration
public class AppConfig {

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

    @Bean
    NoArgGenerator timeBasedEpochGenerator() {
        return Generators.timeBasedEpochGenerator();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
