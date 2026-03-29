package com.minewaku.chatter.apigateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.apigateway.filter.AuthenticationFilter;
import com.minewaku.chatter.apigateway.filter.RequestThrottlingFilter;

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
			.filters(f -> f
				.filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("users", r -> r
			.path("/api/v*/users/**")
			.filters(f -> f
				.filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("roles", r -> r
			.path("/api/v*/roles/**")
			.filters(f -> f
				.filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://AUTHENTICATION-SERVICE")
		)
		.route("profiles", r -> r
			.path("/api/v*/profiles/**")
			.filters(f -> f
				.filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://PROFILES-SERVICE")
		)
		.build();
	}

}
