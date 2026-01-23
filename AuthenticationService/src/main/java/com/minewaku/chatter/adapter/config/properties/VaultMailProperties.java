package com.minewaku.chatter.adapter.config.properties;

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
@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "mail")
public class VaultMailProperties {

    private String username = "dummy";
    private String password = "123456";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        log.info("mail.username from env: " + environment.getProperty("mail.username"));
        log.info("mail.password from env: " + environment.getProperty("mail.password"));
    }
}