package com.minewaku.chatter.adapter.config.properties;

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
public class JwtProperties {

    @Value("${private-key}")
    private String privateKey = "dummy-key";

    @Value("${public-key}")
    private String publicKey = "dummy-key";

    @Autowired
    Environment environment;

    @PostConstruct
    private void test() {
        System.out.println("privateKey: " + privateKey);
        System.out.println("publicKey: " + publicKey);
    }
}
