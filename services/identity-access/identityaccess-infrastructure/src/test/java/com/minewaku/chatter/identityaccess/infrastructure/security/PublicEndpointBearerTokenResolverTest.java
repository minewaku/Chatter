package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

class PublicEndpointBearerTokenResolverTest {

    private static final List<RequestMatcher> PUBLIC = List.of(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v*/auth/refresh"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"));

    private final PublicEndpointBearerTokenResolver resolver = new PublicEndpointBearerTokenResolver(PUBLIC);

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer some.expired.token");
        return request;
    }

    @Test
    void ignoresTheAuthorizationHeaderOnPublicEndpoints() {
        assertThat(resolver.resolve(request("POST", "/api/v1/auth/refresh"))).isNull();
        assertThat(resolver.resolve(request("GET", "/swagger-ui/index.html"))).isNull();
    }

    @Test
    void resolvesTheBearerTokenOnProtectedEndpoints() {
        assertThat(resolver.resolve(request("GET", "/api/v1/sessions"))).isEqualTo("some.expired.token");
    }

    @Test
    void methodSpecificMatchersAreHonoured() {
        PublicEndpointBearerTokenResolver preflightOnly = new PublicEndpointBearerTokenResolver(
                List.of(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.OPTIONS, "/**")));

        assertThat(preflightOnly.resolve(request("OPTIONS", "/api/v1/sessions")))
                .isNull();
        assertThat(preflightOnly.resolve(request("GET", "/api/v1/sessions"))).isEqualTo("some.expired.token");
    }
}
