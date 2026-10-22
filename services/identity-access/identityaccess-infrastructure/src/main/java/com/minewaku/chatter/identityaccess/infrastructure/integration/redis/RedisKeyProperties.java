package com.minewaku.chatter.identityaccess.infrastructure.integration.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Prefix shared by every key this service writes, so other services can share the same Redis instance. */
@ConfigurationProperties("identity-access.redis")
public record RedisKeyProperties(String keyPrefix) {

    public static final String DEFAULT_PREFIX = "identity-access:";

    public RedisKeyProperties {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            keyPrefix = DEFAULT_PREFIX;
        }
        if (!keyPrefix.endsWith(":")) {
            throw new IllegalArgumentException("identity-access.redis.key-prefix must end with ':'");
        }
    }
}
