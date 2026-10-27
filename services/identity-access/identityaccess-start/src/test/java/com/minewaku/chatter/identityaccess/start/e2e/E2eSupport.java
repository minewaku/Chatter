package com.minewaku.chatter.identityaccess.start.e2e;

import com.minewaku.chatter.identityaccess.start.StartApplication;
import com.minewaku.chatter.identityaccess.start.support.VaultContainerSupport;
import com.minewaku.chatter.identityaccess.user.api.command.LockUserAccountUseCase;
import com.minewaku.chatter.identityaccess.user.api.command.UnlockUserAccountUseCase;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for end-to-end tests: the whole service (core, infrastructure, presentation) on a random port against real
 * PostgreSQL 17 and Redis 7 containers shared by all subclasses, with an in-memory mail sender.
 */
@SpringBootTest(
        classes = StartApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            // generous limits so the other end-to-end tests never trip them; ThrottlingE2eTest overrides these
            // with @DynamicPropertySource, which outranks inline properties
            "identity-access.throttle.sign-in.max-attempts=1000",
            "identity-access.throttle.reconfirmation.max-attempts=1000",
            "identity-access.throttle.resend-gap.max-attempts=1000",
            "identity-access.throttle.resend-hourly.max-attempts=1000",
            "identity-access.throttle.email-change-request.max-attempts=1000"
        })
@Import(E2eSupport.MailConfiguration.class)
public abstract class E2eSupport {

    public static final String PASSWORD = "S3cure-pass!";
    public static final String KEY_PREFIX = "e2e:";

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("chatter")
            .withUsername("admin")
            .withPassword("123456");

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
        // the service's signing keys come from Vault only; seed the pair the tests sign with. Spring Cloud Vault reads
        // its connection while the environment is prepared, before @DynamicPropertySource values exist, so the
        // coordinates go in as system properties.
        System.setProperty("spring.cloud.vault.scheme", "http");
        System.setProperty("spring.cloud.vault.host", VaultContainerSupport.host());
        System.setProperty("spring.cloud.vault.port", String.valueOf(VaultContainerSupport.port()));
        System.setProperty("spring.cloud.vault.token", VaultContainerSupport.ROOT_TOKEN);
        VaultContainerSupport.put(
                "common/jwt/rs256", Map.of("private-key", TestKeys.privatePem(), "public-key", TestKeys.publicPem()));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                        + "/chatter?currentSchema=identity_access");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.data.redis.timeout", () -> "1s");

        registry.add("identity-access.redis.key-prefix", () -> KEY_PREFIX);
        registry.add("spring.main.banner-mode", () -> "off");
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected RecordingMailSender mail;

    @Autowired
    protected StringRedisTemplate redis;

    @Autowired
    protected CircuitBreakerRegistry breakers;

    @Autowired
    private LockUserAccountUseCase lockAccount;

    @Autowired
    private UnlockUserAccountUseCase unlockAccount;

    /** Locks the account behind an access credential. Sessions stay alive, as no endpoint locks an account. */
    protected void lockAccountOf(String accessToken) {
        lockAccount.handle(new LockUserAccountUseCase.Command(Long.parseLong(claim(accessToken, "sub"))));
    }

    /** Returns a locked account behind an access credential to active. */
    protected void unlockAccountOf(String accessToken) {
        unlockAccount.handle(new UnlockUserAccountUseCase.Command(Long.parseLong(claim(accessToken, "sub"))));
    }

    /** Closes every breaker, for tests that deliberately open one. */
    protected void resetBreakers() {
        breakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    protected ApiClient api() {
        return new ApiClient(port);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected static String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Registers an account, confirms the emailed token, and returns its email. */
    protected String registerVerifiedAccount() {
        return registerVerifiedAccount(uniqueUsername());
    }

    /** Same as {@link #registerVerifiedAccount()} with a chosen username. */
    protected String registerVerifiedAccount(String username) {
        String email = uniqueEmail();
        ApiResponse registered = api().post(
                        "/auth/register",
                        java.util.Map.of(
                                "email", email, "username", username, "birthday", "1999-01-01", "password", PASSWORD));
        if (registered.status() != 201) {
            throw new IllegalStateException("registration failed with " + registered.status());
        }
        String token = mail.latestTokenFor(email).orElseThrow();
        ApiResponse confirmed = api().get("/auth/verification/confirm?token=" + token, null);
        if (confirmed.status() != 200) {
            throw new IllegalStateException("verification failed with " + confirmed.status());
        }
        return email;
    }

    /** Signs in and returns the {@code [accessToken, refreshToken]} pair. */
    protected String[] signIn(String email) {
        ApiResponse response = api().post("/auth/authenticate", ApiClient.credentials(email, PASSWORD));
        if (response.status() != 200) {
            throw new IllegalStateException("sign-in failed with " + response.status());
        }
        return new String[] {response.text("accessToken"), response.refreshCookie()};
    }

    /** Reads one claim from a JWT without validating it (tests only). */
    protected static String claim(String jwt, String name) {
        String payload = new String(java.util.Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
        return new tools.jackson.databind.ObjectMapper()
                .readTree(payload)
                .get(name)
                .asString();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class MailConfiguration {

        @Bean
        RecordingMailSender mailSender() {
            return new RecordingMailSender();
        }
    }
}
