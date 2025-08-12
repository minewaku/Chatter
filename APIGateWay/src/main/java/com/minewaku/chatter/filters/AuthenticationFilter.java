package com.minewaku.chatter.filters;

import com.minewaku.chatter.exceptions.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String[] WHITELIST = new String[]{
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    };

    private final RestTemplate restTemplate;

    public AuthenticationFilter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String open : WHITELIST) {
            if (path.startsWith(open)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new UnauthorizedException("Authorization header is missing or invalid");
        }

        String token = authHeader.substring(7);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            // Delegate to AuthService for token validation
            ResponseEntity<Void> validateResponse = restTemplate.exchange(
                    "http://AUTH-SERVICE/api/auth/validate",
                    HttpMethod.GET,
                    httpEntity,
                    Void.class
            );
            if (!validateResponse.getStatusCode().is2xxSuccessful()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                throw new UnauthorizedException("Invalid token");
            }
        } catch (RestClientException ex) {
            log.warn("Auth validation failed: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new UnauthorizedException("Unauthorized");
        }

        filterChain.doFilter(request, response);
    }
}