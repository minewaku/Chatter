package com.minewaku.chatter.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt.public-key")
public class JwtProperties {
	private String publicKey = "dummy-key";
}