package com.minewaku.chatter.identityaccess.infrastructure.integration.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shows, in the health endpoint, whether mail and the session store are in use or being held off. A breaker that is
 * not closed reports {@code DEGRADED} rather than {@code DOWN}: the service still runs and an orchestrator must not
 * restart it because a mail provider is slow. Reports no details, so nothing about credentials or recipients can leak.
 *
 * <p>The component names are the bean names without the {@code HealthIndicator} suffix: {@code mailDelivery} and
 * {@code sessionStore}.
 */
@Configuration
public class DependencyHealthIndicators {

    static final String DEGRADED = "DEGRADED";

    @Bean
    HealthIndicator mailDeliveryHealthIndicator(@Qualifier(ResilienceConfiguration.MAIL) CircuitBreaker breaker) {
        return () -> report(breaker);
    }

    @Bean
    HealthIndicator sessionStoreHealthIndicator(
            @Qualifier(ResilienceConfiguration.SESSION_STORE) CircuitBreaker breaker) {
        return () -> report(breaker);
    }

    private static Health report(CircuitBreaker breaker) {
        return (breaker.getState() == CircuitBreaker.State.CLOSED ? Health.up() : Health.status(DEGRADED)).build();
    }
}
