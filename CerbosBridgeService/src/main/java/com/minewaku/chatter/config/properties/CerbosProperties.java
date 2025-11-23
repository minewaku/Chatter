package com.minewaku.chatter.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "cerbos")
public class CerbosProperties {
    private String username = "dummy";
    private String password = "123456";
}
