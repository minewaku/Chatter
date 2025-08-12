package com.minewaku.chatter.filters;

import com.minewaku.chatter.exceptions.UnauthorizedException;
import com.minewaku.chatter.utils.JWTUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String[] WHITELIST = new String[]{
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    };

    public static final String REQUEST_ATTR_USERNAME = "AUTHENTICATED_USERNAME";

    private final JWTUtils jwtUtils;

    public AuthenticationFilter(JWTUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
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
        Optional<Claims> claimsOptional = jwtUtils.parseClaims(token);
        if (claimsOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new UnauthorizedException("Invalid token");
        }

        Claims claims = claimsOptional.get();
        String username = claims.getSubject();
        if (username == null || username.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new UnauthorizedException("Invalid token subject");
        }

        request.setAttribute(REQUEST_ATTR_USERNAME, username);
        filterChain.doFilter(request, response);
    }
}