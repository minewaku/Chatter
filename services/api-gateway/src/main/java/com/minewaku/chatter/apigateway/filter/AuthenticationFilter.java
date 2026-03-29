package com.minewaku.chatter.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.apigateway.constant.ExchangeAttr;
import com.minewaku.chatter.apigateway.exception.InvalidTokenException;
import com.minewaku.chatter.apigateway.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtils;

    public AuthenticationFilter(
            JwtUtil jwtUtils) {

        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = extractBearerToken(authHeader);

            log.info("incoming request: " + config.toString());

            if (token == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return jwtUtils.parseClaims(token)
                    .flatMap(claims -> {
                        String username = claims.get("email", String.class);
                        Long userId = Long.parseLong(claims.getSubject());

                        if (username == null || username.isBlank()) {
                            log.warn("Invalid username in token");
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        ExchangeAttr.USERNAME.put(exchange, username);
                        ExchangeAttr.USER_ID.put(exchange, userId);

                        return chain.filter(exchange);
                    })
                    .onErrorResume(InvalidTokenException.class, e -> {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    private String extractBearerToken(String header) {
        if (header == null || !header.startsWith("Bearer "))
            return null;
        return header.substring(7);
    }

    public static class Config {

    }
}
