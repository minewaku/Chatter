package com.minewaku.chatter.profile.infrastructure.storage.cloudinary.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {

    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private boolean secure;

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        log.info("cloudName from env: {}",environment.getProperty("cloudinary.cloud-name"));
        log.info("apiKey from env: {}",environment.getProperty("cloudinary.api-key"));
        log.info("apiSecret from env: {}",environment.getProperty("cloudinary.api-secret"));
        log.info("secure from env: {}",environment.getProperty("cloudinary.secure"));
    }
}
