package com.minewaku.chatter.identityaccess.infrastructure.security;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints that prove the security chain: one protected, one public (registration stub). */
@RestController
public class SecurityProbeController {

    public final AtomicInteger invocations = new AtomicInteger();

    @GetMapping("/probe/protected")
    String protectedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        invocations.incrementAndGet();
        return jwt.getSubject();
    }

    @PostMapping("/api/v1/auth/register")
    String register() {
        invocations.incrementAndGet();
        return "registered";
    }
}
