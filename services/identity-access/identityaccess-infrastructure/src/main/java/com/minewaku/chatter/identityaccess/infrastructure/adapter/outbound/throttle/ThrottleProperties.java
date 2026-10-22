package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.throttle;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One limit per kind of action: at most {@code maxAttempts} within {@code window}. The window is also how long a
 * refused subject waits. Anything left out takes the default shown in each accessor.
 */
@ConfigurationProperties("identity-access.throttle")
public record ThrottleProperties(
        Limit signIn, Limit reconfirmation, Limit resendGap, Limit resendHourly, Limit emailChangeRequest) {

    public record Limit(Integer maxAttempts, Duration window) {}

    public ThrottleProperties {
        signIn = checked("sign-in", signIn, 5, Duration.ofMinutes(15));
        reconfirmation = checked("reconfirmation", reconfirmation, 5, Duration.ofMinutes(15));
        resendGap = checked("resend-gap", resendGap, 1, Duration.ofMinutes(1));
        resendHourly = checked("resend-hourly", resendHourly, 5, Duration.ofHours(1));
        emailChangeRequest = checked("email-change-request", emailChangeRequest, 3, Duration.ofHours(1));
    }

    private static Limit checked(String name, Limit configured, int defaultMax, Duration defaultWindow) {
        int maxAttempts =
                configured == null || configured.maxAttempts() == null ? defaultMax : configured.maxAttempts();
        Duration window = configured == null || configured.window() == null ? defaultWindow : configured.window();
        String prefix = "identity-access.throttle." + name;
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(prefix + ".max-attempts must be at least 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException(prefix + ".window must be positive");
        }
        return new Limit(maxAttempts, window);
    }
}
