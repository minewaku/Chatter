package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code lifetime} is how long a refresh session stays usable; {@code retention} is the extra time an expired or
 * revoked session stays readable before Redis removes it.
 */
@ConfigurationProperties("identity-access.session")
public record SessionProperties(Duration lifetime, Duration retention) {

    public SessionProperties {
        if (lifetime == null) {
            lifetime = Duration.ofDays(30);
        }
        if (retention == null) {
            retention = Duration.ofDays(30);
        }
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("identity-access.session.lifetime must be positive");
        }
        if (retention.isNegative()) {
            throw new IllegalArgumentException("identity-access.session.retention must not be negative");
        }
    }
}
