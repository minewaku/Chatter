package com.minewaku.chatter.adapter.service;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import io.jsonwebtoken.Claims;

public interface IAccessTokenVerifier {
    Claims extractClaims(String token);
    JwtDecoder getJwtDecoder();
}
