package com.minewaku.chatter.profile.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless resource-server protection: tokens are validated by the framework (see {@link JwtDecoderConfiguration});
 * there is no custom authentication filter and no revoked-token check (spec 012 FR-006).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String HEALTH_ENDPOINTS = "/actuator/health/**";

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * {@code entryPoint} and {@code accessDeniedHandler} are optional: the presentation module supplies beans that
     * write the standard error body; without them the framework defaults apply.
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            CorsConfigurationSource corsConfigurationSource,
            ObjectProvider<AuthenticationEntryPoint> entryPoint,
            ObjectProvider<AccessDeniedHandler> accessDeniedHandler)
            throws Exception {
        String[] publicEndpoints = properties.publicEndpoints().toArray(String[]::new);
        BearerTokenResolver bearerTokenResolver =
                new PublicEndpointBearerTokenResolver(ignoredBearerEndpoints(properties));

        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(HttpMethod.OPTIONS, "/**")
                            .permitAll()
                            .requestMatchers(HEALTH_ENDPOINTS)
                            .permitAll();
                    if (publicEndpoints.length > 0) {
                        authorize.requestMatchers(publicEndpoints).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> {
                    oauth2.jwt(Customizer.withDefaults()).bearerTokenResolver(bearerTokenResolver);
                    entryPoint.ifAvailable(oauth2::authenticationEntryPoint);
                    accessDeniedHandler.ifAvailable(oauth2::accessDeniedHandler);
                });

        return http.build();
    }

    /** Public endpoints, health checks, and preflight requests never have their bearer token validated. */
    private static List<RequestMatcher> ignoredBearerEndpoints(SecurityProperties properties) {
        PathPatternRequestMatcher.Builder matchers = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> ignored = new ArrayList<>();
        ignored.add(matchers.matcher(HttpMethod.OPTIONS, "/**"));
        ignored.add(matchers.matcher(HEALTH_ENDPOINTS));
        properties.publicEndpoints().forEach(pattern -> ignored.add(matchers.matcher(pattern)));
        return ignored;
    }
}
