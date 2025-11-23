package com.minewaku.chatter.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.filters.AuthenticationFilter;
import com.minewaku.chatter.filters.RequestThrottlingFilter;


@Configuration
public class GatewayConfig {

    @Bean
    RouteLocator customRoutes(
        RouteLocatorBuilder builder, 
        AuthenticationFilter authenticationFilter,
        RequestThrottlingFilter requestThrottlingFilter) {

        return builder.routes()
            .route("authentication-service", r -> r
                .path("/api/*/authentication/register", "/api/*/authentication/authenticate", 
                        "/api/*/authentication/refresh", "/api/*/authentication/change-password", 
                        "/api/*/authentication/verify", "/api/*/authentication/resend-verify-email",
                        "/api/*/authentication/logout")
                    .filters(f -> f
                    .filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
                )
                .uri("lb://AUTHENTICATION-SERVICE")
            )
            .route("authentication-service", r -> r
                .path("/api/*/authentication/**")
                    .filters(f -> f
                    .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                    .filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
                )
                .uri("lb://AUTHENTICATION-SERVICE")
            )
            .route("authorization-service", r -> r
                .path("/api/*/authorization/**")
                .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                .uri("lb://AUTHORIZATION-SERVICE")
            )
            .build();
    }

}
