package com.minewaku.chatter.resolver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.jwt.Jwt;

import com.minewaku.chatter.web.request.PipRequest;

public interface AttributeResolver {
    Set<String> supportedAttributes();

    @Async("resolverExecutor")
    CompletableFuture<Map<String, Object>> resolveAsync(Jwt jwt, PipRequest request);
}
