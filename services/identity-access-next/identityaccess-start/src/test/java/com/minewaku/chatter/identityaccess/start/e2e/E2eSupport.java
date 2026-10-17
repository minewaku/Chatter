package com.minewaku.chatter.identityaccess.start.e2e;

import com.minewaku.chatter.identityaccess.start.StartApplication;
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
@SpringBootTest(classes = StartApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        registry.add("spring.data.redis.timeout", () -> "2s");
        registry.add("identity-access.redis.key-prefix", () -> KEY_PREFIX);
        registry.add("identity-access.security.jwt.private-key", TestKeys::privatePem);
        registry.add("identity-access.security.jwt.public-key", TestKeys::publicPem);
        registry.add("spring.main.banner-mode", () -> "off");
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected RecordingMailSender mail;

    @Autowired
    protected StringRedisTemplate redis;

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
        String email = uniqueEmail();
        ApiResponse registered = api().post(
                        "/auth/register",
                        java.util.Map.of(
                                "email",
                                email,
                                "username",
                                uniqueUsername(),
                                "birthday",
                                "1999-01-01",
                                "password",
                                PASSWORD));
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
        return new String[] {response.text("accessToken"), response.text("refreshToken")};
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
