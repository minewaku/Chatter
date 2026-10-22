package com.minewaku.chatter.identityaccess.infrastructure.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class DependencyHealthIndicatorsTest {

    @Configuration
    @EnableConfigurationProperties(ResilienceProperties.class)
    @Import({ResilienceConfiguration.class, DependencyHealthIndicators.class})
    static class TestConfig {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void bothComponentsAreUpWhileTheirBreakersAreClosed() {
        runner.run(context -> {
            assertThat(context.getBean("mailDeliveryHealthIndicator", HealthIndicator.class)
                            .health()
                            .getStatus())
                    .isEqualTo(Status.UP);
            assertThat(context.getBean("sessionStoreHealthIndicator", HealthIndicator.class)
                            .health()
                            .getStatus())
                    .isEqualTo(Status.UP);
        });
    }

    @Test
    void anOpenOrHalfOpenBreakerIsDegradedNotDown() {
        runner.run(context -> {
            CircuitBreaker mail = context.getBean(ResilienceConfiguration.MAIL, CircuitBreaker.class);
            HealthIndicator indicator = context.getBean("mailDeliveryHealthIndicator", HealthIndicator.class);

            mail.transitionToOpenState();
            assertThat(indicator.health().getStatus()).isEqualTo(new Status("DEGRADED"));

            mail.transitionToHalfOpenState();
            assertThat(indicator.health().getStatus()).isEqualTo(new Status("DEGRADED"));

            mail.transitionToClosedState();
            assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        });
    }

    @Test
    void eachComponentFollowsItsOwnBreaker() {
        runner.run(context -> {
            context.getBean(ResilienceConfiguration.SESSION_STORE, CircuitBreaker.class)
                    .transitionToOpenState();

            assertThat(context.getBean("mailDeliveryHealthIndicator", HealthIndicator.class)
                            .health()
                            .getStatus())
                    .isEqualTo(Status.UP);
            assertThat(context.getBean("sessionStoreHealthIndicator", HealthIndicator.class)
                            .health()
                            .getStatus())
                    .isEqualTo(new Status("DEGRADED"));
        });
    }

    @Test
    void theReportCarriesNoDetails() {
        runner.run(context -> {
            context.getBean(ResilienceConfiguration.MAIL, CircuitBreaker.class).transitionToOpenState();

            Health health = context.getBean("mailDeliveryHealthIndicator", HealthIndicator.class)
                    .health();

            assertThat(health.getDetails()).isEmpty();
        });
    }
}
