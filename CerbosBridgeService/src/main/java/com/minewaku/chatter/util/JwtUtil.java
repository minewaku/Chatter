package com.minewaku.chatter.util;

import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class JwtUtil {

    // Các claim chuẩn JWT cần loại bỏ
    private static final Set<String> STANDARD_JWT_CLAIMS = Set.of(
        "iss", "sub", "aud", "exp", "nbf", "iat", "jti"
    );

    private static final String ROLES_CLAIM = "roles";

    public Map<String, Object> getCustomClaims(Jwt jwt) {
        Map<String, Object> allClaims = jwt.getClaims();

        allClaims.keySet().removeAll(STANDARD_JWT_CLAIMS);
        allClaims.remove(ROLES_CLAIM);

        return allClaims;
    }
}