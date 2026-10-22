package com.minewaku.chatter.identityaccess.shared.throttle;

import java.util.Objects;

/** Every limit the services apply. Core only names the type; infrastructure provides the bean from configuration. */
public record ThrottlePolicies(
        AttemptPolicy signIn,
        AttemptPolicy reconfirmation,
        AttemptPolicy resendGap,
        AttemptPolicy resendHourly,
        AttemptPolicy emailChangeRequest) {

    public ThrottlePolicies {
        Objects.requireNonNull(signIn);
        Objects.requireNonNull(reconfirmation);
        Objects.requireNonNull(resendGap);
        Objects.requireNonNull(resendHourly);
        Objects.requireNonNull(emailChangeRequest);
    }
}
