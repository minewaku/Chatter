package com.minewaku.chatter.identityaccess.presentation;

import static org.mockito.Mockito.mock;

import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAccessDeniedHandler;
import com.minewaku.chatter.identityaccess.presentation.shared.error.ApiAuthenticationEntryPoint;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * A small stand-in for the infrastructure security chain used by the web-slice tests: the same public/protected
 * split as the real allow-list, bearer-token handling with this module's error beans, and a stub decoder so tests
 * authenticate with {@code SecurityMockMvcRequestPostProcessors.jwt()}.
 */
@TestConfiguration
@EnableWebSecurity
public class SliceSecurityConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }

    @Bean
    SecurityFilterChain sliceChain(
            HttpSecurity http, ApiAuthenticationEntryPoint entryPoint, ApiAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/v*/auth/register",
                                "/api/v*/auth/authenticate",
                                "/api/v*/auth/refresh",
                                "/api/v*/auth/logout",
                                "/api/v*/auth/verification/resend",
                                "/api/v*/auth/verification/confirm")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
