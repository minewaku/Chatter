package com.minewaku.chatter.identityaccess.infrastructure.integration.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ResiliencePropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ResilienceProperties.class)
    @Import(ResilienceConfiguration.class)
    static class TestConfig {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void defaultsMatchTheDocumentedSettings() {
        runner.run(context -> {
            ResilienceProperties properties = context.getBean(ResilienceProperties.class);

            assertThat(properties.mail())
                    .isEqualTo(new ResilienceProperties.Breaker(10, 5, 50, Duration.ofSeconds(30), 2));
            assertThat(properties.sessionStore())
                    .isEqualTo(new ResilienceProperties.Breaker(20, 10, 50, Duration.ofSeconds(10), 3));
        });
    }

    @Test
    void buildsTwoNamedBreakersFromTheSettings() {
        runner.withPropertyValues(
                        "identity-access.resilience.mail.window-size=4",
                        "identity-access.resilience.mail.minimum-calls=2")
                .run(context -> {
                    CircuitBreaker mail = context.getBean(ResilienceConfiguration.MAIL, CircuitBreaker.class);
                    CircuitBreaker store = context.getBean(ResilienceConfiguration.SESSION_STORE, CircuitBreaker.class);

                    assertThat(mail.getCircuitBreakerConfig().getSlidingWindowSize())
                            .isEqualTo(4);
                    assertThat(mail.getCircuitBreakerConfig().getMinimumNumberOfCalls())
                            .isEqualTo(2);
                    assertThat(store.getCircuitBreakerConfig().getSlidingWindowSize())
                            .isEqualTo(20);
                    assertThat(mail.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
                });
    }

    @Test
    void exposesNoDurationBean() {
        runner.run(context -> assertThat(context.getBeansOfType(Duration.class)).isEmpty());
    }

    @Test
    void everyInvalidValueStopsStartupAndNamesTheProperty() {
        String[][] cases = {
            {"identity-access.resilience.mail.window-size=0", "mail.window-size"},
            {"identity-access.resilience.mail.minimum-calls=11", "mail.minimum-calls"},
            {"identity-access.resilience.mail.minimum-calls=0", "mail.minimum-calls"},
            {"identity-access.resilience.session-store.failure-rate-percent=0", "session-store.failure-rate-percent"},
            {"identity-access.resilience.session-store.failure-rate-percent=101", "session-store.failure-rate-percent"},
            {"identity-access.resilience.session-store.open-duration=PT0S", "session-store.open-duration"},
            {"identity-access.resilience.mail.trial-calls=0", "mail.trial-calls"},
        };
        for (String[] invalid : cases) {
            runner.withPropertyValues(invalid[0]).run(context -> assertThat(context)
                    .as(invalid[0])
                    .hasFailed()
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining(invalid[1]));
        }
    }
}
