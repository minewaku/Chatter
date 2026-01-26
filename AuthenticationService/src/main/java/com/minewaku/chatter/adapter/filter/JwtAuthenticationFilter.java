package com.minewaku.chatter.adapter.filter;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.minewaku.chatter.adapter.service.IAccessTokenVerifier;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final IAccessTokenVerifier accessTokenVerifier;

    public JwtAuthenticationFilter(
            IAccessTokenVerifier accessTokenVerifier) {

        this.accessTokenVerifier = accessTokenVerifier;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwtString;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtString = authHeader.substring(7);
            Claims claims = accessTokenVerifier.extractClaims(jwtString);
            String userId = claims.getSubject();

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                JwtDecoder jwtDecoder = accessTokenVerifier.getJwtDecoder();
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
