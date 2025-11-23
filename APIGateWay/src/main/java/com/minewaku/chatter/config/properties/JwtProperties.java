package com.minewaku.chatter.config.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
public class JwtProperties {
    @Value("${public-key}")
    private String publicKey;

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        System.out.println("publicKey: " + publicKey);
    }
}
