package com.minewaku.chatter.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JWTUtils {

    @Value("${jwt.secret:change-me-please-change-me-please-change-me}")
    private String secret;

    @Value("${jwt.username-claim:sub}")
    private String usernameClaim;

    private SecretKey key() {
        // HS256 requires at least 256-bit key
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> parseClaims(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> extractUsername(String token) {
        return parseClaims(token).map(claims -> {
            Object value = claims.get(usernameClaim);
            return value != null ? String.valueOf(value) : claims.getSubject();
        });
    }
}