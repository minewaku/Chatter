package com.minewaku.chatter.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.minewaku.chatter.resolver.AttributeResolver;
import com.minewaku.chatter.web.request.PipRequest;

@Service
public class AttributeResolverRegistry {

    private final List<AttributeResolver> resolvers;

    public AttributeResolverRegistry(List<AttributeResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public CompletableFuture<Map<String, Object>> resolveAll(Jwt jwt, PipRequest request) {

        //Each AttributeResolver can provide multiple attrs at a time, so this map represents for mapping between resolver/attrs
        Map<AttributeResolver, Set<String>> grouped = new HashMap<>();
        Set<String> requestedAttributes = request.requestedAttributes();

        for (String attr : requestedAttributes) {
            resolvers.stream()
                .filter(r -> r.supportedAttributes().contains(attr))
                .findFirst()
                .ifPresent(resolver -> {
                    grouped.computeIfAbsent(resolver, k -> new HashSet<>()).add(attr);
                });
        }
        
        // Build a list of futures, one per resolver
        List<CompletableFuture<Map<String, Object>>> futures = grouped.entrySet().stream()
                .map(entry -> {
                    AttributeResolver resolver = entry.getKey();

                    return resolver.resolveAsync(jwt, request);
                })
                .toList();

        // Wait for *all* futures and merge their maps
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> {
                    // v means Void (null)
                    Map<String, Object> result = new HashMap<>();
                    for (var f : futures) {
                        result.putAll(f.join());
                    }
                    return result;
                });

    }
}
