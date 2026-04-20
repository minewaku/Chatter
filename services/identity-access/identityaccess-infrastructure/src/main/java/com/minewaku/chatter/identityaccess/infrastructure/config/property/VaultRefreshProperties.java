package com.minewaku.chatter.identityaccess.infrastructure.config.property;

import org.springframework.beans.factory.annotation.Autowired;
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
    
    private String password = "dummy-key";
    private String salt = "dummy-key";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        log.info("password bound to field: " + password);
        log.info("salt bound to field: " + salt);
    }
}