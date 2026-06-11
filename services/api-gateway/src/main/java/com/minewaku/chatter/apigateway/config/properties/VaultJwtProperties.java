package com.minewaku.chatter.apigateway.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@Component
@RequiredArgsConstructor
public class VaultJwtProperties {
    @Value("${public-key}")
    private String publicKey = "dummy-key";

    private final Environment environment;

    @PostConstruct
    private void test() {
        log.info("publicKey from env: " + environment.getProperty("public-key"));
    }
}
