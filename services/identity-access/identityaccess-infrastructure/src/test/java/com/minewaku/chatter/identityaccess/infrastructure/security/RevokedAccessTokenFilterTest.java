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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
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
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringJUnitWebConfig(RevokedAccessTokenFilterTest.Config.class)
class RevokedAccessTokenFilterTest extends RedisTestSupport {

    static final KeyPair PAIR = SecurityChainConfig.PAIR;
    static final String PREFIX = "filter-test:";

    /** A Redis template whose revocation lookup can be switched off to simulate an unreachable store. */
    static final class SwitchableRedisTemplate extends StringRedisTemplate {
        volatile boolean down;
        final java.util.concurrent.atomic.AtomicInteger lookups = new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public Boolean hasKey(String key) {
            lookups.incrementAndGet();
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

        @Bean("sessionStoreBreaker")
        CircuitBreaker sessionStoreBreaker() {
            return CircuitBreaker.of(
                    "filter-test",
                    CircuitBreakerConfig.custom()
                            .slidingWindowSize(2)
                            .minimumNumberOfCalls(2)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(30))
                            .build());
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

    @Autowired
    WebApplicationContext context;

    @Autowired
    SwitchableRedisTemplate redis;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("sessionStoreBreaker") CircuitBreaker breaker;

    @Autowired
    SecurityProbeController probe;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        redis.down = false;
        redis.lookups.set(0);
        breaker.reset();
        flushAll(redis);
        probe.invocations.set(0);
    }

    private static String token(String sessionId) {
        var props = new JwtProperties("identity-access", "chatter", Duration.ofMinutes(15), null);
        return new JwtAccessTokenIssuer(RsaKeyFixtures.keys(PAIR), props, Clock.fixed(Instant.now(), ZoneOffset.UTC))
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
    void afterRepeatedFailuresTheStoreIsNotAskedAtAllAndRequestsStayRefused() throws Exception {
        redis.down = true;
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("any-session")))
                    .andExpect(status().is(503));
        }
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int lookupsWhenOpened = redis.lookups.get();

        // even a store that has come back is not asked while the breaker holds off, and nothing gets through
        redis.down = false;
        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("any-session")))
                .andExpect(status().is(503));

        assertThat(redis.lookups.get()).isEqualTo(lookupsWhenOpened);
        assertThat(probe.invocations.get()).isZero();
    }

    @Test
    void aRevokedSessionIsASuccessfulLookupAndDoesNotCountAgainstTheBreaker() throws Exception {
        revoke("revoked-session");

        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("revoked-session")))
                    .andExpect(status().isUnauthorized());
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void aHealthyStoreAndAClosedBreakerStillPassAValidCredential() throws Exception {
        mvc.perform(get("/probe/protected").header("Authorization", "Bearer " + token("fine-session")))
                .andExpect(status().isOk());

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void requestsWithoutAnyCredentialAreUntouchedByTheFilter() throws Exception {
        redis.down = true;

        mvc.perform(get("/probe/protected")).andExpect(status().isUnauthorized());
    }
}
