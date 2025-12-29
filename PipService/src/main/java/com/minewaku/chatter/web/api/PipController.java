package com.minewaku.chatter.web.api;

import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minewaku.chatter.service.impl.AttributeResolverRegistry;
import com.minewaku.chatter.web.request.PipRequest;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/")
public class PipController {
    private final AttributeResolverRegistry registry;

    public PipController(AttributeResolverRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/attributes")
    public Mono<Map<String, Object>> getAttributes(@RequestBody PipRequest request) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = auth.getToken();

        return Mono.fromFuture(registry.resolveAll(jwt, request));
    }
}
