package com.minewaku.chatter.resolver;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.minewaku.chatter.web.request.PipRequest;

@Service
public class TestResovler implements AttributeResolver {
    
    private WebClient webClient;

    public TestResovler(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Set<String> supportedAttributes() {
        return Set.of("user.avartarUrl", "user.coverUrl", "user.displayName", "user.description");
    }

    private Map<String, Object> toMap(AttrsReponse resp) {
        return Map.of(
            "email", resp.avatarUrl(), 
            "coverUrl", resp.coverUrl(),
            "displayName", resp.displayName(),
            "description", resp.description());
    }

    private record AttrsReponse(
            String avatarUrl, 
            String coverUrl, 
            String displayName, 
            String description)
    {}

    @Override
    @Async("resolverExecutor")
    public CompletableFuture<Map<String, Object>> resolveAsync(Jwt jwt, PipRequest pipRequest) {
        String subject = jwt.getSubject();

        String url = "http://chatter.profiles-service/users/" + subject;
        String token = jwt.getTokenValue();

        return webClient.get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(AttrsReponse.class)
            .map(this::toMap)
            .onErrorReturn(Collections.emptyMap())
            .toFuture();
    }
}
