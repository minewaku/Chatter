package com.minewaku.chatter.apigateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.apigateway.filter.RequestThrottlingFilter;

@Configuration
public class AuthenticationRoute {

    @Bean
    RouteLocator authenticationRouteLocator(
            RouteLocatorBuilder builder,
			RequestThrottlingFilter requestThrottlingFilter
    ) {

    return builder.routes()
		//IDENTITYACCESS
		.route("auth", r -> r
			.path("/api/v*/auth/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://IDENTITYACCESS")
		)
		.route("user", r -> r
			.path("/api/v*/users/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://IDENTITYACCESS")
		)
		.route("session", r -> r
			.path("/api/v*/sessions/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://IDENTITYACCESS")
		)

		//PROFILE
		.route("avatar webhook", r -> r
			.path("/api/v*/webhooks/cloudinary/avatars/**")
			.uri("lb://PROFILE")
		)
		.route("banner webhook webhook", r -> r
			.path("/api/v*/webhooks/cloudinary/banners/**")
			.uri("lb://PROFILE")
		)
		.route("profile webhooks", r -> r
			.path("/api/v*/profiles/webhooks/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://PROFILE")
		)
		.route("profile", r -> r
			.path("/api/v*/profiles/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://PROFILE")
		)

		//MESSSAGE
		.route("attachment webhook", r -> r
			.path("/api/v*/webhooks/cloudinary/attachments/**")
			.uri("lb://MESSAGE")
		)
		.route("guild icon webhook", r -> r
			.path("/api/v*/webhooks/cloudinary/guild-icons/**")
			.uri("lb://MESSAGE")
		)
		.route("guild", r -> r
			.path("/api/v*/guilds/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://MESSAGE")
		)
		.route("invite", r -> r
			.path("/api/v*/invites/**")
			.filters(f -> f
				.filter(requestThrottlingFilter.apply(new RequestThrottlingFilter.Config()))
			)
			.uri("lb://MESSAGE")
		)
		.build();
	}

}
