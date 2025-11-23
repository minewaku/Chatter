package com.minewaku.chatter.adapter.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {
    private String username = "dummy";
    private String password = "123456";

    @PostConstruct
    private void test() {
        System.out.println("mail.username: " + username);
        System.out.println("mail.password: " + password);
    }
}
