package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class SessionConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(SessionProperties.class)
    @Import(SessionConfiguration.class)
    static class TestConfig {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void exposesExactlyOneDurationBeanWithTheConfiguredLifetime() {
        runner.withPropertyValues("identity-access.session.lifetime=PT2H").run(context -> {
            assertThat(context.getBeansOfType(Duration.class)).hasSize(1).containsKey("sessionLifetime");
            assertThat(context.getBean("sessionLifetime", Duration.class)).isEqualTo(Duration.ofHours(2));
        });
    }

    @Test
    void defaultsToThirtyDaysWhenNothingIsConfigured() {
        runner.run(context -> {
            assertThat(context.getBean("sessionLifetime", Duration.class)).isEqualTo(Duration.ofDays(30));
            assertThat(context.getBean(SessionProperties.class).retention()).isEqualTo(Duration.ofDays(30));
        });
    }
}
