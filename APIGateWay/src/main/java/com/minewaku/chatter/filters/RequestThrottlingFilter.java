package com.minewaku.chatter.filters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.config.RateLimitConfig;
import com.minewaku.chatter.constants.ExchangeAttr;
import com.minewaku.chatter.models.RateLimitRule;
import com.minewaku.chatter.services.IRateLimitService;
import com.minewaku.chatter.utils.HttpClientUtil;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class RequestThrottlingFilter extends AbstractGatewayFilterFactory<RequestThrottlingFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(RequestThrottlingFilter.class);

    private final IRateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;
    private final HttpClientUtil httpClientUtil;
    private final ObjectMapper objectMapper;

    public RequestThrottlingFilter(
        IRateLimitService rateLimitService,
        RateLimitConfig rateLimitConfig,
        HttpClientUtil httpClientUtil,
        ObjectMapper objectMapper) {
            
        super(Config.class);
        this.rateLimitService = rateLimitService;
        this.rateLimitConfig = rateLimitConfig;
        this.httpClientUtil = httpClientUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            String username = ExchangeAttr.USERNAME.get(exchange);
            List<String> roles = objectMapper.convertValue(
                ExchangeAttr.ROLES.get(exchange),
                new TypeReference<List<String>>() {});
            
            log.info("Incoming request: {}", config.toString());

            RateLimitRule matchedRule = rateLimitConfig.getRules().stream()
                    .filter(rule -> rule.pathMatches(path))
                    .filter(rule -> rule.rolesMatches(roles.stream().map(String::toUpperCase).toList()))
                    .findFirst()
                    .orElse(null);

            if (matchedRule == null) {
                return chain.filter(exchange);
            }

            String key = (username != null)
                    ? username
                    : httpClientUtil.getClientIP(exchange.getRequest());

            Bandwidth limit = Bandwidth.builder()
                    .capacity(matchedRule.getCapacity())
                    .refillIntervally(matchedRule.getRefilltokens(), matchedRule.getPeriod())
                    .initialTokens(matchedRule.getInitialTokens())
                    .build();

            Bucket bucket = rateLimitService.redisBucket(key, limit);

            return Mono.fromCallable(() -> bucket.tryConsumeAndReturnRemaining(1))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(probe -> {
                        if (probe.isConsumed()) {
                            return chain.filter(exchange);
                        } else {
                            log.warn("Rate limit exceeded for key: {}", key);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().add("Retry-After",
                                    String.valueOf(matchedRule.getPeriod().getSeconds()));
                            return exchange.getResponse().setComplete();
                        }
                    });
        };
    }

    public static class Config {
        
    }
}
