package com.minewaku.chatter.identityaccess.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validates incoming access credentials against the public key of the signing pair (a {@link PemKeys.RsaKeyPair}
 * bean supplied by the application module, which reads it from Vault): signature, expiry, not-before and the configured issuer. The audience is
 * deliberately not checked; there is one issuer, one key pair and one audience for the whole platform, so the check
 * could never reject a real request.
 */
@Configuration
public class JwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties, PemKeys.RsaKeyPair keys) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
