package com.minewaku.chatter.adapter.config.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "redis")
public class VaultRedisProperties {

    private static final Logger logger = LoggerFactory.getLogger(VaultRedisProperties.class);

    private String username = "dummy";
    private String password = "123456";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        logger.info("redis.username from env: " + environment.getProperty("redis.username"));
        logger.info("redis.password from env: " + environment.getProperty("redis.password"));
    }
}
