package com.minewaku.chatter.adapter.config.service;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class AppConfig {
    
    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties dsp = new DataSourceProperties();
        return dsp;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSourceProperties dataSourceProperties) {
        return new JdbcTemplate(dataSourceProperties.initializeDataSourceBuilder().build());
    }

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean 
	SpelExpressionParser spelExpressionParser() {
		return new SpelExpressionParser();
	}

    @Bean 
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
