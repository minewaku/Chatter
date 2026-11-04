package com.minewaku.chatter.profilenext.presentation.support;

import com.minewaku.chatter.profilenext.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.profilenext.presentation.shared.error.ApiAuthenticationEntryPoint;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The same shape as the real security chain (stateless resource server, a public webhook path, everything else
 * authenticated) so controller tests can show 401 without a token and use the {@code jwt()} post-processor.
 */
@TestConfiguration
@EnableWebSecurity
@Import({ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class})
public class TestSecurityConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    @Bean
    SecurityFilterChain testSecurityFilterChain(
            HttpSecurity http, ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDenied)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/profiles/uploads/notifications")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied));
        return http.build();
    }
}
