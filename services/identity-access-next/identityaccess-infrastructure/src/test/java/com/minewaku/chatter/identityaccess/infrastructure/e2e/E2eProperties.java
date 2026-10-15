package com.minewaku.chatter.identityaccess.infrastructure.e2e;

import com.minewaku.chatter.identityaccess.infrastructure.support.PostgreSqlTestSupport;
import com.minewaku.chatter.identityaccess.infrastructure.support.RedisTestSupport;
import com.minewaku.chatter.identityaccess.infrastructure.support.RsaKeyFixtures;
import java.security.KeyPair;
import java.util.LinkedHashMap;
import java.util.Map;

/** Properties that point a full application context at the Testcontainers PostgreSQL and Redis instances. */
public final class E2eProperties {

    public static final String KEY_PREFIX = "e2e:";

    private static final KeyPair KEYS = RsaKeyFixtures.newKeyPair();

    private E2eProperties() {}

    public static Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spring.datasource.url", PostgreSqlTestSupport.jdbcUrl());
        values.put("spring.datasource.username", PostgreSqlTestSupport.username());
        values.put("spring.datasource.password", PostgreSqlTestSupport.password());
        values.put("spring.flyway.enabled", "true");
        values.put("spring.flyway.schemas", "identity_access");
        values.put("spring.flyway.default-schema", "identity_access");
        values.put("spring.flyway.create-schemas", "true");
        values.put("spring.data.redis.host", RedisTestSupport.redisHost());
        values.put("spring.data.redis.port", String.valueOf(RedisTestSupport.redisPort()));
        values.put("identity-access.redis.key-prefix", KEY_PREFIX);
        values.put("identity-access.security.jwt.private-key", RsaKeyFixtures.privatePem(KEYS));
        values.put("identity-access.security.jwt.public-key", RsaKeyFixtures.publicPem(KEYS));
        values.put("management.endpoints.web.exposure.include", "health");
        values.put("management.health.mail.enabled", "false");
        values.put("spring.main.banner-mode", "off");
        return values;
    }
}
