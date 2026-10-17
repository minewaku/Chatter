package com.minewaku.chatter.identityaccess.presentation.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class SecurityErrorHandlersTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(MAPPER);
    private final ApiAccessDeniedHandler denied = new ApiAccessDeniedHandler(MAPPER);

    private static JsonNode json(MockHttpServletResponse response) throws Exception {
        return MAPPER.readTree(response.getContentAsString());
    }

    @Test
    void anyInvalidOrMissingCredentialIsUnauthenticatedWithABearerChallenge() throws Exception {
        for (RuntimeException failure : new RuntimeException[] {
            new InvalidBearerTokenException("secret reason"), new BadCredentialsException("secret")
        }) {
            MockHttpServletResponse response = new MockHttpServletResponse();

            entryPoint.commence(
                    new MockHttpServletRequest(), response, (org.springframework.security.core.AuthenticationException)
                            failure);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(json(response).get("code").asString()).isEqualTo("unauthenticated");
            assertThat(response.getContentAsString()).doesNotContain("secret");
        }
    }

    @Test
    void aCredentialStoreFailureIsServiceUnavailableWithARetryHint() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                new MockHttpServletRequest(), response, new AuthenticationServiceException("secret store down"));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
        assertThat(json(response).get("code").asString()).isEqualTo("service_unavailable");
        assertThat(response.getContentAsString()).doesNotContain("secret");
    }

    @Test
    void accessDeniedIsForbiddenInTheStandardShape() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        denied.handle(new MockHttpServletRequest(), response, new AccessDeniedException("secret"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = json(response);
        assertThat(body.get("code").asString()).isEqualTo("forbidden");
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("timestamp").asString()).isNotBlank();
        assertThat(response.getContentAsString()).doesNotContain("secret");
    }
}
