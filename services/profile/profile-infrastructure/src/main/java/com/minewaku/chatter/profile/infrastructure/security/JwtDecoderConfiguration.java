package com.minewaku.chatter.profile.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validates incoming access tokens against the platform's public key (a {@link PemKeys.PublicKeyHolder} bean supplied
 * by the application module, which reads it from Vault): signature, expiry, not-before and the configured issuer. The
 * audience is deliberately not checked.
 */
@Configuration
public class JwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties, PemKeys.PublicKeyHolder keys) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
