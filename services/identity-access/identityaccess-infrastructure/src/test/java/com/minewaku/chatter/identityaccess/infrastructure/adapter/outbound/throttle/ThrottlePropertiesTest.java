package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.throttle;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import com.minewaku.chatter.identityaccess.shared.throttle.ThrottlePolicies;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ThrottlePropertiesTest {

    @Configuration
    @EnableConfigurationProperties(ThrottleProperties.class)
    @Import(ThrottleConfiguration.class)
    static class TestConfig {}

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void defaultsMatchTheDocumentedLimits() {
        runner.run(context -> {
            ThrottlePolicies policies = context.getBean(ThrottlePolicies.class);

            assertThat(policies.signIn()).isEqualTo(new AttemptPolicy("sign-in", 5, Duration.ofMinutes(15)));
            assertThat(policies.reconfirmation())
                    .isEqualTo(new AttemptPolicy("reconfirmation", 5, Duration.ofMinutes(15)));
            assertThat(policies.resendGap()).isEqualTo(new AttemptPolicy("resend-gap", 1, Duration.ofMinutes(1)));
            assertThat(policies.resendHourly()).isEqualTo(new AttemptPolicy("resend-hourly", 5, Duration.ofHours(1)));
            assertThat(policies.emailChangeRequest())
                    .isEqualTo(new AttemptPolicy("email-change-request", 3, Duration.ofHours(1)));
        });
    }

    @Test
    void everyPolicyHasItsOwnName() {
        runner.run(context -> {
            ThrottlePolicies p = context.getBean(ThrottlePolicies.class);

            List<String> names = List.of(
                    p.signIn().name(),
                    p.reconfirmation().name(),
                    p.resendGap().name(),
                    p.resendHourly().name(),
                    p.emailChangeRequest().name());
            assertThat(names).doesNotHaveDuplicates();
        });
    }

    @Test
    void configuredValuesReplaceTheDefaultsOneFieldAtATime() {
        runner.withPropertyValues(
                        "identity-access.throttle.sign-in.max-attempts=2",
                        "identity-access.throttle.resend-gap.window=PT5S")
                .run(context -> {
                    ThrottlePolicies policies = context.getBean(ThrottlePolicies.class);

                    assertThat(policies.signIn().maxAttempts()).isEqualTo(2);
                    assertThat(policies.signIn().window()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(policies.resendGap().maxAttempts()).isEqualTo(1);
                    assertThat(policies.resendGap().window()).isEqualTo(Duration.ofSeconds(5));
                });
    }

    @Test
    void exposesNoDurationBean() {
        runner.run(context -> assertThat(context.getBeansOfType(Duration.class)).isEmpty());
    }

    @Test
    void aZeroMaximumStopsStartupAndNamesTheProperty() {
        runner.withPropertyValues("identity-access.throttle.reconfirmation.max-attempts=0")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("identity-access.throttle.reconfirmation.max-attempts"));
    }

    @Test
    void aZeroWindowStopsStartupAndNamesTheProperty() {
        runner.withPropertyValues("identity-access.throttle.email-change-request.window=PT0S")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("identity-access.throttle.email-change-request.window"));
    }
}
