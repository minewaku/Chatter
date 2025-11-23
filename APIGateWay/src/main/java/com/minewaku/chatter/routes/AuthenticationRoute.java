package com.minewaku.chatter.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.filters.AuthenticationFilter;
import com.minewaku.chatter.filters.RequestThrottlingFilter;

@Configuration
public class AuthenticationRoute {

    String[] PUBLIC_PATHS = {
        "/api/*/authentication/register",
        "/api/*/authentication/authenticate",
        "/api/*/authentication/refresh",
        "/api/*/authentication/verify",
        "/api/*/authentication/resend-verify-email",
    };

@Bean
RouteLocator authenticationRouteLocator(
    RouteLocatorBuilder builder, 
    AuthenticationFilter authenticationFilter,
    RequestThrottlingFilter requestThrottlingFilter) {

    return builder.routes()
        .route("authentication-auth-public", r -> r.path(PUBLIC_PATHS)
            .filters(f -> f.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config())))
            .uri("lb://AUTHENTICATION-SERVICE"))
        .route("authentication-secured", r -> r.path("/**")
            .filters(f -> f
                .filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
                .filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
            .uri("lb://AUTHENTICATION-SERVICE"))
        .build();
    }

}
