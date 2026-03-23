package com.minewaku.chatter.identityaccess.infrastructure.config.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "refresh")
public class VaultRefreshProperties {
    
    @Value("${password}")
    private String password = "dummy-key";

    @Value("${salt}")
    private String salt = "dummy-key";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        log.info("password from env: " + environment.getProperty("password"));
        log.info("salt from env: " + environment.getProperty("salt"));
    }
}
