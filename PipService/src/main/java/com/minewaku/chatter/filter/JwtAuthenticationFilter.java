package com.minewaku.chatter.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.exception.ApiException;
import com.minewaku.chatter.service.IJwtService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final ObjectMapper objectMapper;
    private final IJwtService jwtService;

    public JwtAuthenticationFilter(
        ObjectMapper objectMapper,
        IJwtService jwtService
    ) {
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    private void sendErrorResponse(HttpServletResponse response, ApiException errorDetail) throws IOException {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        ErrorResponse errorResponse = ErrorResponse.builder(errorDetail, status, "An error has occurred").build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(errorResponse));
            writer.flush();
        }
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwtString; 

        if(authHeader == null || !authHeader.startsWith("Bearer")) {
            ApiException apiException = new ApiException("Invalid JWT Token", "ERR01",HttpStatus.UNAUTHORIZED); 
        	sendErrorResponse(response, apiException);
            return;
        }
        
        try {
        	jwtString = authHeader.substring(7);
            Claims claims = jwtService.extractClaims(jwtString);
            String userId = claims.getSubject();

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                //create and set the authentication object (this object will be able to use everywhere in the application during that thread request)
                JwtDecoder jwtDecoder = jwtService.getJwtDecoder();
                Jwt jwt = jwtDecoder.decode(jwtString);
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            } else {
                sendErrorResponse(response, new ApiException("Invalid JWT Token", "ERR01", HttpStatus.UNAUTHORIZED));
            }
		} catch (Exception e) {
			log.error("Error: ", e);
			sendErrorResponse(response, new ApiException("Invalid JWT Token", "ERR01", HttpStatus.UNAUTHORIZED));
		}
    }
}
