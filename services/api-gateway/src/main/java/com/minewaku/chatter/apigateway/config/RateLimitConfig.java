package com.minewaku.chatter.apigateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import com.minewaku.chatter.apigateway.model.RateLimitPolicy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Validated
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit", ignoreUnknownFields = false)
public class RateLimitConfig {

    @NotEmpty(message = "rules must not be empty")
    @Valid
    private List<RateLimitPolicy> rules;

    public List<RateLimitPolicy> getRules() {
        return rules;
    }

    public void setRules(List<RateLimitPolicy> rules) {
        this.rules = rules;
    }
}
