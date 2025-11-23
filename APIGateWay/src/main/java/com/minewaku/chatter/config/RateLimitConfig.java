package com.minewaku.chatter.config;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.models.RateLimitRule;

@Component
public class RateLimitConfig {

    public List<RateLimitRule> getRules() {
        return List.of(
            RateLimitRule.builder().pathPattern("api/v1/auth/**").isApplied(true).roles(null).initialTokens(10).capacity(10).refilltokens(10).period(Duration.ofMinutes(1)).build(),
            RateLimitRule.builder().pathPattern("api/admin/v1/user/**").isApplied(true).roles(List.of("ADMIN")).initialTokens(100).capacity(100).refilltokens(100).period(Duration.ofMinutes(1)).build(),
            RateLimitRule.builder().pathPattern("api/v1/auth/**").isApplied(true).roles(List.of("USER")).initialTokens(100).capacity(100).refilltokens(100).period(Duration.ofMinutes(1)).build(),
            RateLimitRule.builder().pathPattern("api/index").isApplied(false).build()
        );
    }
}
