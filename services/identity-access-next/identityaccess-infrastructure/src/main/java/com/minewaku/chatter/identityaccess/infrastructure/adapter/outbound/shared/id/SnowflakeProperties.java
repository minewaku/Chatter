package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.shared.id;

import java.time.Instant;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("identity-access.snowflake")
public record SnowflakeProperties(int workerId, Instant epoch) {

    public static final Instant DEFAULT_EPOCH = Instant.parse("2026-01-01T00:00:00Z");

    public SnowflakeProperties {
        if (epoch == null) {
            epoch = DEFAULT_EPOCH;
        }
    }
}
