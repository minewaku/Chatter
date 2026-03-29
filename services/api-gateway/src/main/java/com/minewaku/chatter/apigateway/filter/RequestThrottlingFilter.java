package com.minewaku.chatter.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.apigateway.config.RateLimitConfig;
import com.minewaku.chatter.apigateway.constant.ExchangeAttr;
import com.minewaku.chatter.apigateway.model.RateLimitPolicy;
import com.minewaku.chatter.apigateway.service.IRateLimitService;
import com.minewaku.chatter.apigateway.util.HttpClientUtil;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Log4j2
@Component
public class RequestThrottlingFilter extends AbstractGatewayFilterFactory<RequestThrottlingFilter.Config> {

    private final IRateLimitService rateLimitService;

    private final RateLimitConfig rateLimitConfig;
    private final HttpClientUtil httpClientUtil;

    public RequestThrottlingFilter(
        IRateLimitService rateLimitService,
        RateLimitConfig rateLimitConfig,
        HttpClientUtil httpClientUtil) {
            
        super(Config.class);
        this.rateLimitService = rateLimitService;
        this.rateLimitConfig = rateLimitConfig; 
        this.httpClientUtil = httpClientUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            String username = ExchangeAttr.USERNAME.get(exchange);
            
            log.info("Incoming request: {}", config.toString());

            RateLimitPolicy matchedRule = rateLimitConfig.getRules().stream()
                    .filter(rule -> rule.pathMatches(path))
                    .findFirst()
                    .orElse(null);

            if (matchedRule == null) {
                return chain.filter(exchange);
            }

            String key = (username != null)
                    ? username
                    : httpClientUtil.getClientIP(exchange.getRequest());

            Bandwidth limit = matchedRule.getIsGreedyRefill() ? 
                Bandwidth.builder()
                    .capacity(matchedRule.getCapacity())
                    .refillGreedy(matchedRule.getRefilltokens(), matchedRule.getPeriod())
                    .initialTokens(matchedRule.getInitialTokens())
                    .build() :
                Bandwidth.builder()
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