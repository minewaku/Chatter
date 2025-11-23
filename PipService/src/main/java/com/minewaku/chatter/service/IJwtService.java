package com.minewaku.chatter.service;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import io.jsonwebtoken.Claims;

public interface IJwtService {
    Claims extractClaims(String token);
    JwtDecoder getJwtDecoder();
}
