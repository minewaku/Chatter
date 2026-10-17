package com.minewaku.chatter.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.authentication.JwtAccessTokenIssuer;
import com.minewaku.chatter.identityaccess.infrastructure.integration.redis.RedisKeyProperties;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringJUnitWebConfig(RevokedAccessTokenFilterTest.Config.class)
class RevokedAccessTokenFilterTest extends RedisTestSupport {

    static final KeyPair PAIR = RsaKeyFixtures.newKeyPair();
    static final String PREFIX = "filter-test:";

    /** A Redis template whose revocation lookup can be switched off to simulate an unreachable store. */
    static final class SwitchableRedisTemplate extends StringRedisTemplate {
        volatile boolean down;

        @Override
        public Boolean hasKey(String key) {
            if (down) {
                throw new RedisConnectionFailureException("store unavailable");
            }
            return super.hasKey(key);
        }
    }

    @Configuration
    @Import(SecurityChainConfig.class)
    static class Config {

        @Bean
        SwitchableRedisTemplate redisTemplate() {
            StringRedisTemplate real = RedisTestSupport.newTemplate();
            SwitchableRedisTemplate template = new SwitchableRedisTemplate();
            template.setConnectionFactory(real.getConnectionFactory());
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        RedisKeyProperties redisKeyProperties() {
            return new RedisKeyProperties(PREFIX);
        }

        /** Mirrors the presentation entry point: 503 for a credential-store failure, 401 otherwise. */
        @Bean
        AuthenticationEntryPoint entryPoint() {
            return (request, response, exception) ->
                    response.setStatus(exception instanceof AuthenticationServiceException ? 503 : 401);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("identity-access.security.jwt.private-key", () -> RsaKeyFixtures.privatePem(PAIR));
        registry.add("identity-access.security.jwt.public-key", () -> RsaKeyFixtures.publicPem(PAIR));
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    SwitchableRedisTemplate redis;

    @Autowired
    SecurityProbeController probe;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        redis.down = false;
        flushAll(redis);
        probe.invocations.set(0);
    }

    private static String token(String sessionId) {
        var props = new JwtProperties(
                RsaKeyFixtures.privatePem(PAIR),
                RsaKeyFixtures.publicPem(PAIR),
                "identity-access",
                "chatter",
                Duration.ofMinutes(15));
        return new JwtAccessTokenIssuer(props, Clock.fixed(Instant.now(), ZoneOffset.UTC))
                .issue(new UserId(4242L), new Email("alice@example.com"), sessionId);
    }

    private static String tokenWithoutSessionClaim() {
        RSAKey jwk = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) PAIR.getPublic())
                .privateKey(PAIR.getPrivate())
                .build();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("identity-access")
                .audience(List.of("chatter"))
                .subject("4242")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    private void revoke(String sessionId) {
        redis.opsForValue().set(PREFIX + "access-revoked:4242:" + sessionId, "1", 60, TimeUnit.SECONDS);
    }

    @Test
    void aRevokedSessionsAccessTokenIsRefusedAndNoEndpointLogicRuns() throws Exception {
        revoke("revoked-session");

        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("revoked-session")))
                .andExpect(status().isUnauthorized());

        assertThat(probe.invocations.get()).isZero();
    }

    @Test
    void anotherSessionOfTheSameAccountIsUnaffected() throws Exception {
        revoke("revoked-session");

        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("other-session")))
                .andExpect(status().isOk())
                .andExpect(content().string("4242"));
    }

    @Test
    void aTokenWithoutASessionClaimIsRefused() throws Exception {
        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + tokenWithoutSessionClaim()))
                .andExpect(status().isUnauthorized());

        assertThat(probe.invocations.get()).isZero();
    }

    @Test
    void anUnreachableRevocationStoreFailsClosedOnProtectedEndpointsOnly() throws Exception {
        redis.down = true;

        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("any-session")))
                .andExpect(status().is(503));
        assertThat(probe.invocations.get()).isZero();

        mvc.perform(post("/api/v1/auth/register")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/register").header("Authorization", "Bearer " + token("any-session")))
                .andExpect(status().isOk());
    }

    @Test
    void requestsWithoutAnyCredentialAreUntouchedByTheFilter() throws Exception {
        redis.down = true;

        mvc.perform(get("/probe/protected")).andExpect(status().isUnauthorized());
    }
}
