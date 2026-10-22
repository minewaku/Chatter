package com.minewaku.chatter.identityaccess.infrastructure.security;

import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses access credentials of revoked sessions. Runs after the framework has validated the signature, issuer,
 * audience and expiry. A session revocation writes the entry {@code <prefix>access-revoked:<userId>:<sessionId>}
 * that lives as long as the longest access credential it could affect.
 *
 * <p>A credential without a session claim, or whose session has an entry, gets {@code 401}. If the entry cannot be
 * read the request fails closed with an {@link AuthenticationServiceException}, which the entry point turns into
 * {@code 503}. Never logs the credential.
 *
 * <p>When a breaker is given, the lookup runs inside it: after repeated failures the store is not asked at all for a
 * while and requests fail at once with the same {@code 503}. An open breaker is a failure to read the entry, so it can
 * never let a request through.
 *
 * <p>Deliberately not a bean: a {@code Filter} bean would also be registered with the servlet container. It is
 * created inside {@link SecurityConfiguration} and added to the security chain only.
 */
final class RevokedAccessTokenFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final AuthenticationEntryPoint entryPoint;
    private final CircuitBreaker breaker;

    RevokedAccessTokenFilter(StringRedisTemplate redis, RedisKeyProperties keys, AuthenticationEntryPoint entryPoint) {
        this(redis, keys, entryPoint, null);
    }

    RevokedAccessTokenFilter(
            StringRedisTemplate redis,
            RedisKeyProperties keys,
            AuthenticationEntryPoint entryPoint,
            CircuitBreaker breaker) {
        this.redis = redis;
        this.keyPrefix = keys.keyPrefix() + "access-revoked:";
        this.entryPoint = entryPoint;
        this.breaker = breaker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            chain.doFilter(request, response);
            return;
        }
        Jwt jwt = token.getToken();
        String sessionId = jwt.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) {
            reject(request, response, new InvalidBearerTokenException("Missing session"));
            return;
        }
        boolean revoked;
        try {
            String key = keyPrefix + jwt.getSubject() + ":" + sessionId;
            revoked = Boolean.TRUE.equals(
                    breaker == null ? redis.hasKey(key) : breaker.executeSupplier(() -> redis.hasKey(key)));
        } catch (RuntimeException e) {
            reject(request, response, new AuthenticationServiceException("Revocation state unavailable", e));
            return;
        }
        if (revoked) {
            reject(request, response, new InvalidBearerTokenException("Revoked session"));
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        entryPoint.commence(request, response, exception);
    }
}
