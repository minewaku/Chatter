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
@ConfigurationProperties(prefix = "cloudinary")
public class VaultCloudinaryProperties {

    private static final Logger logger = LoggerFactory.getLogger(VaultCloudinaryProperties.class);

    private String cloud_name = "dummy";
    private String api_key = "123456";
    private String api_secret = "123456";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        logger.info("cloudinary.cloud_name from env: " + environment.getProperty("cloudinary.cloud_name"));
        logger.info("cloudinary.api_key from env: " + environment.getProperty("cloudinary.api_key"));
        logger.info("cloudinary.api_secret from env: " + environment.getProperty("cloudinary.api_secret"));
    }
}