package com.minewaku.chatter.adapter.config.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.minewaku.chatter.adapter.filter.GlobalLoggingFilter;
import com.minewaku.chatter.adapter.filter.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final GlobalLoggingFilter globalLoggingFilter;
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(GlobalLoggingFilter globalLoggingFilter,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.globalLoggingFilter = globalLoggingFilter;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        public static final String[] PUBLIC_ENDPOINTS = {
                        "/api/v*/auth/authenticate",
                        "/api/v*/auth/register",
                        "/api/v*/auth/verification/resend",
                        "/api/v*/auth/verification/confirm",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
        };

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable());

                return http
                                .authorizeHttpRequests(requests -> requests
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(management -> management
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(globalLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        @Bean
        UrlBasedCorsConfigurationSource corsConfigurationSource() {

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.addAllowedOrigin("https://example.com");
                configuration.addAllowedMethod("*");
                configuration.addAllowedHeader("*");
                configuration.setAllowCredentials(true);
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
