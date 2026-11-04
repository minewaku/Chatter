package com.minewaku.chatter.profilenext.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Ignores the {@code Authorization} header on public endpoints. Without this the framework validates any bearer
 * token it sees and answers {@code 401} for a bad one even on a permitted path, such as the provider's webhook.
 */
public final class PublicEndpointBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final List<RequestMatcher> publicEndpoints;

    public PublicEndpointBearerTokenResolver(List<RequestMatcher> publicEndpoints) {
        this.publicEndpoints = List.copyOf(publicEndpoints);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        for (RequestMatcher matcher : publicEndpoints) {
            if (matcher.matches(request)) {
                return null;
            }
        }
        return delegate.resolve(request);
    }
}
