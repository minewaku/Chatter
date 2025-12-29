package com.minewaku.chatter.filters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.constants.ExchangeAttr;
import com.minewaku.chatter.exceptions.InvalidTokenException;
import com.minewaku.chatter.utils.JwtUtil;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtUtil jwtUtils;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(
            JwtUtil jwtUtils,
            ObjectMapper objectMapper) {

        super(Config.class);
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
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
                        List<String> roles = objectMapper.convertValue(
                                claims.get("roles"),
                                new TypeReference<List<String>>() {
                                });

                        if (username == null || username.isBlank()) {
                            log.warn("Invalid username in token");
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        ExchangeAttr.USERNAME.put(exchange, username);
                        ExchangeAttr.USER_ID.put(exchange, userId);
                        ExchangeAttr.ROLES.put(exchange, java.util.Arrays.asList(roles));

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
