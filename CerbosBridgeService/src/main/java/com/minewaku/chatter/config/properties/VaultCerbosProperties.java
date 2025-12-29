package com.minewaku.chatter.config.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "cerbos")
public class VaultCerbosProperties {
    private String username = "dummy";
    private String password = "123456";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        System.out.println("cerbos.username from env: " + environment.getProperty("cerbos.username"));
        System.out.println("cerbos.password from env: " + environment.getProperty("cerbos.password"));
    }
}
