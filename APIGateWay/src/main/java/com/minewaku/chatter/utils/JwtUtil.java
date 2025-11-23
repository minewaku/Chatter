package com.minewaku.chatter.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.config.properties.JwtProperties;
import com.minewaku.chatter.exceptions.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class JwtUtil {

    private final PublicKey publicKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.publicKey = loadPublicKey(jwtProperties.getPublicKey());
    }

    // ---------- PUBLIC METHODS ---------- //

    public <T> Mono<T> extractExtra(String token, String key, Class<T> type) {
        return extractClaim(token, claims -> claims.get(key, type));
    }

    public Mono<String> extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    public Mono<String> extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Mono<Set<String>> extractAudience(String token) {
        return extractClaim(token, Claims::getAudience);
    }

    public Mono<Date> extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Mono<Date> extractNotBefore(String token) {
        return extractClaim(token, Claims::getNotBefore);
    }

    public Mono<Date> extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public Mono<String> extractId(String token) {
        return extractClaim(token, Claims::getId);
    }

    // ---------- INTERNAL IMPLEMENTATION ---------- //

    private <T> Mono<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        return parseClaims(token).map(claimsResolver);
    }

    public Mono<Claims> parseClaims(String token) {
        return Mono.fromCallable(() ->
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
        ).onErrorMap(JwtException.class,
            e -> new InvalidTokenException("Invalid or expired token", e)
        );
    }

    private PublicKey loadPublicKey(String pem) {
        try {
            // Remove PEM header/footer and whitespaces
            String cleanPem = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(cleanPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }
}
