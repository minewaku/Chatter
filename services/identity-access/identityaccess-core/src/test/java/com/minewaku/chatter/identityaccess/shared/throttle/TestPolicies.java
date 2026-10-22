package com.minewaku.chatter.identityaccess.shared.throttle;

import java.time.Duration;

/** The documented default limits, for tests that build a service by hand. */
public final class TestPolicies {

    private TestPolicies() {}

    public static ThrottlePolicies defaults() {
        return new ThrottlePolicies(
                new AttemptPolicy("sign-in", 5, Duration.ofMinutes(15)),
                new AttemptPolicy("reconfirmation", 5, Duration.ofMinutes(15)),
                new AttemptPolicy("resend-gap", 1, Duration.ofMinutes(1)),
                new AttemptPolicy("resend-hourly", 5, Duration.ofHours(1)),
                new AttemptPolicy("email-change-request", 3, Duration.ofHours(1)));
    }

    /** Limits so high that a test about something else never reaches them. */
    public static ThrottlePolicies generous() {
        AttemptPolicy any = new AttemptPolicy("generous", 1000, Duration.ofHours(1));
        return new ThrottlePolicies(
                named("sign-in", any),
                named("reconfirmation", any),
                named("resend-gap", any),
                named("resend-hourly", any),
                named("email-change-request", any));
    }

    private static AttemptPolicy named(String name, AttemptPolicy like) {
        return new AttemptPolicy(name, like.maxAttempts(), like.window());
    }
}
