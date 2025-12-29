package com.minewaku.chatter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

    // Các claim chuẩn JWT cần loại bỏ
    private static final Set<String> STANDARD_JWT_CLAIMS = Set.of(
            "iss", "sub", "aud", "exp", "nbf", "iat", "jti");

    private static final String ROLES_CLAIM = "roles";

    public Map<String, Object> getCustomClaims(Jwt jwt) {
        Map<String, Object> customClaims = new HashMap<>(jwt.getClaims());

        customClaims.keySet().removeAll(STANDARD_JWT_CLAIMS);
        customClaims.remove(ROLES_CLAIM);

        return customClaims;
    }
}