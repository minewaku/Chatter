package com.minewaku.chatter.adapter.config.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Component
public class VaultJwtProperties {

    private static final Logger logger = LoggerFactory.getLogger(VaultJwtProperties.class);

    @Value("${private-key}")
    private String privateKey = "dummy-key";

    @Value("${public-key}")
    private String publicKey = "dummy-key";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        logger.info("privateKey from env: " + environment.getProperty("private-key"));
        logger.info("publicKey from env: " + environment.getProperty("public-key"));
    }
}
