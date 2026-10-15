package com.minewaku.chatter.identityaccess.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.minewaku.chatter.identityaccess")
@ConfigurationPropertiesScan("com.minewaku.chatter.identityaccess")
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class, args);
    }
}
