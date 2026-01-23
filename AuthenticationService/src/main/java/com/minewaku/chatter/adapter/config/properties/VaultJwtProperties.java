package com.minewaku.chatter.adapter.config.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class VaultJwtProperties {

    @Value("${private-key}")
    private String privateKey = "dummy-key";

    @Value("${public-key}")
    private String publicKey = "dummy-key";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        log.info("privateKey from env: " + environment.getProperty("private-key"));
        log.info("publicKey from env: " + environment.getProperty("public-key"));
    }
}
