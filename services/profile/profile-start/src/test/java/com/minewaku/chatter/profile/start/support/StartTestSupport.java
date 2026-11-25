package com.minewaku.chatter.profile.start.support;

import com.minewaku.chatter.profile.infrastructure.adapter.outbound.image.storage.r2.ObjectStoreClient;
import com.minewaku.chatter.profile.infrastructure.security.PemKeys;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that start the whole service against real PostgreSQL 17 and Kafka. Vault and Eureka are
 * switched off and the token public key comes from a throwaway key pair, so tests can sign their own tokens.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.vault.enabled=false", "eureka.client.enabled=false"})
@Import(StartTestSupport.TestOverrides.class)
public abstract class StartTestSupport {

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("chatter")
            .withUsername("admin")
            .withPassword("123456");

    protected static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    protected static final String ACCOUNT_TOPIC = "identityaccess.user.v1";

    protected static final RsaKeyFixtures KEYS = new RsaKeyFixtures();

    protected static final FakeObjectStore STORAGE = new FakeObjectStore();

    protected static final String BUCKET = "media";

    static {
        POSTGRES.start();
        KAFKA.start();
        createTopics();
    }

    /** The topics exist ahead of the service, with the partition count the dead-letter route relies on. */
    private static void createTopics() {
        java.util.Properties admin = new java.util.Properties();
        admin.put("bootstrap.servers", KAFKA.getBootstrapServers());
        try (org.apache.kafka.clients.admin.AdminClient client = org.apache.kafka.clients.admin.AdminClient.create(admin)) {
            client.createTopics(java.util.List.of(
                            new org.apache.kafka.clients.admin.NewTopic(ACCOUNT_TOPIC, 3, (short) 1),
                            new org.apache.kafka.clients.admin.NewTopic(ACCOUNT_TOPIC + ".profile.dlt", 3, (short) 1)))
                    .all()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @LocalServerPort
    protected int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
                        + "/chatter?currentSchema=profile");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // configured (fake) credentials, a fast sweeper, and a quick retry for a delete that failed
        registry.add("profile.images.r2.account-id", () -> "test-account");
        registry.add("profile.images.r2.bucket", () -> BUCKET);
        registry.add("profile.images.r2.access-key", () -> "test-access");
        registry.add("profile.images.r2.secret-key", () -> "test-secret");
        registry.add("profile.images.public-base-url", () -> "https://media.test");
        registry.add("profile.images.sweep.interval", () -> "PT0.5S");
        registry.add("profile.images.sweep.retry-delay", () -> "PT1S");
    }

    @TestConfiguration
    static class TestOverrides {

        @Bean
        @Primary
        ObjectStoreClient fakeObjectStore() {
            return STORAGE;
        }

        @Bean
        @Primary
        PemKeys.PublicKeyHolder testPublicKey() {
            return new PemKeys.PublicKeyHolder(KEYS.publicKey());
        }
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected static String bearer(long userId) {
        return "Bearer " + KEYS.token(userId, Instant.now().plusSeconds(600));
    }

    /** One request; {@code authorization} may be null. */
    protected HttpResponse<String> send(String method, String path, String authorization, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url(path)));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
