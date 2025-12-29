package com.minewaku.chatter.adapter.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.minewaku.chatter.adapter.service.impl.Rs256JwtTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final Rs256JwtTokenProvider rs256JwtTokenProvider;

    public JwtAuthenticationFilter(
            Rs256JwtTokenProvider rs256JwtTokenProvider) {

        this.rs256JwtTokenProvider = rs256JwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        log.info("Incoming request path AUTHENTICATION-SERVICE: " + path);

        final String authHeader = request.getHeader("Authorization");
        final String jwtString;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtString = authHeader.substring(7);
            Claims claims = rs256JwtTokenProvider.extractClaims(jwtString);
            String userId = claims.getSubject();

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                JwtDecoder jwtDecoder = rs256JwtTokenProvider.getJwtDecoder();
                Jwt jwt = jwtDecoder.decode(jwtString);
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);   
            }
        } catch (MalformedJwtException e) {
            log.error("Failed to parse or validate JWT token: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
        return;
    }
}
