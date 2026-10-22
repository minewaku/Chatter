package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.throttle;

import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import com.minewaku.chatter.identityaccess.shared.throttle.ThrottlePolicies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the one {@link ThrottlePolicies} bean the core services take. Adds no {@code Duration} bean. */
@Configuration
public class ThrottleConfiguration {

    @Bean
    ThrottlePolicies throttlePolicies(ThrottleProperties properties) {
        return new ThrottlePolicies(
                policy("sign-in", properties.signIn()),
                policy("reconfirmation", properties.reconfirmation()),
                policy("resend-gap", properties.resendGap()),
                policy("resend-hourly", properties.resendHourly()),
                policy("email-change-request", properties.emailChangeRequest()));
    }

    private static AttemptPolicy policy(String name, ThrottleProperties.Limit limit) {
        return new AttemptPolicy(name, limit.maxAttempts(), limit.window());
    }
}
