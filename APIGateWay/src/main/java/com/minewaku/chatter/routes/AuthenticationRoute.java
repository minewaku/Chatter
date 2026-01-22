package com.minewaku.chatter.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.filters.AuthenticationFilter;
import com.minewaku.chatter.filters.RequestThrottlingFilter;

@Configuration
public class AuthenticationRoute {

    @Bean
    RouteLocator authenticationRouteLocator(
            RouteLocatorBuilder builder,
            AuthenticationFilter authenticationFilter, 
            RequestThrottlingFilter requestThrottlingFilter
    ) {

    return builder.routes()
		.route("auth", r -> r
			.path("/api/v*/auth/**")
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("users", r -> r
			.path("/api/v*/users/**")
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("roles", r -> r
			.path("/api/v*/roles/**")
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("profiles", r -> r
			.path("/api/v*/profiles/**")
			.uri("lb://PROFILES-SERVICE")
		)
		.build();
	}

}
