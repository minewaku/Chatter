package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication.JwtAccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringJUnitWebConfig(SecurityChainConfig.class)
class SecurityChainTest {

    static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("identity-access.security.jwt.private-key", () -> RsaKeyFixtures.privatePem(PAIR));
        registry.add("identity-access.security.jwt.public-key", () -> RsaKeyFixtures.publicPem(PAIR));
        registry.add("identity-access.security.cors.allowed-origins", () -> "https://app.example.com");
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    SecurityProbeController probe;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        probe.invocations.set(0);
    }

    private static String token(KeyPair signingKey, String issuer, String audience, Instant at) {
        var props = new com.minewaku.chatter.identityaccess.infrastructure.security.JwtProperties(
                RsaKeyFixtures.privatePem(signingKey),
                RsaKeyFixtures.publicPem(signingKey),
                issuer,
                audience,
                Duration.ofMinutes(15));
        return new JwtAccessTokenIssuer(props, Clock.fixed(at, ZoneOffset.UTC))
                .issue(new UserId(4242L), new Email("alice@example.com"));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void validCredentialReachesTheEndpointAndIdentityIsAvailable() throws Exception {
        mvc.perform(get("/probe/protected")
                        .header("Authorization", "Bearer " + token(PAIR, "identity-access", "chatter", now())))
                .andExpect(status().isOk())
                .andExpect(content().string("4242"));
        assertThat(probe.invocations.get()).isEqualTo(1);
    }

    @Test
    void missingMalformedExpiredTamperedForeignAndMismatchedCredentialsAreUnauthorized() throws Exception {
        String valid = token(PAIR, "identity-access", "chatter", now());
        String[] parts = valid.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, 5) + (parts[1].charAt(5) == 'A' ? 'B' : 'A')
                + parts[1].substring(6) + "." + parts[2];
        KeyPair other = RsaKeyFixtures.newKeyPair();

        String[] bad = {
            null,
            "not-a-jwt",
            token(PAIR, "identity-access", "chatter", now().minus(Duration.ofHours(1))),
            tampered,
            token(other, "identity-access", "chatter", now()),
            token(PAIR, "someone-else", "chatter", now()),
            token(PAIR, "identity-access", "another-app", now())
        };
        for (String credential : bad) {
            MockMvcRequestBuilders.class.getName();
            var request = get("/probe/protected");
            if (credential != null) {
                request.header("Authorization", "Bearer " + credential);
            }
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
        assertThat(probe.invocations.get()).isZero();
    }

    @Test
    void publicEndpointNeedsNoCredential() throws Exception {
        mvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isOk())
                .andExpect(content().string("registered"));
    }

    @Test
    void everyDefaultPublicEndpointPassesTheSecurityChainWithoutACredential() throws Exception {
        String[] paths = {
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/verification/resend",
            "/api/v1/auth/verification/confirm",
            "/swagger-ui/index.html",
            "/swagger-ui.html",
            "/v3/api-docs/swagger-config",
            "/actuator/health"
        };
        for (String path : paths) {
            MvcResult result = mvc.perform(post(path)).andReturn();
            assertThat(result.getResponse().getStatus()).as(path).isNotIn(401, 403);
        }
    }

    @Test
    void anyOtherEndpointRequiresACredential() throws Exception {
        mvc.perform(get("/api/v1/users/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void preflightFromAnAllowedOriginIsAnsweredWithoutACredential() throws Exception {
        mvc.perform(options("/probe/protected")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"));
        assertThat(probe.invocations.get()).isZero();
    }

    @Test
    void preflightFromAnUnknownOriginIsDenied() throws Exception {
        MvcResult result = mvc.perform(options("/probe/protected")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isNull();
    }

    @Test
    void theChainIsStatelessAndNeedsNoCsrfTokenForABearerCall() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        assertThat(result.getRequest().getSession(false)).isNull();
    }
}
