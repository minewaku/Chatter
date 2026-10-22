package com.minewaku.chatter.identityaccess.user.internal.application;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.port.AttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptOutcome;
import com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy;
import com.minewaku.chatter.identityaccess.shared.throttle.ThrottlePolicies;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Limits how often a signed-in person may confirm an action with their current password, so a stolen but still valid
 * access credential cannot be used to guess it. One count per person is shared by every action that asks for the
 * password. The attempt is counted before the action runs; only a wrong password keeps the count, any other outcome
 * (a success, or a conflict reached after the password was accepted) clears it.
 */
@Component
public class ReauthenticationGuard {

    private final AttemptLimiter attempts;
    private final AttemptPolicy policy;

    public ReauthenticationGuard(AttemptLimiter attempts, ThrottlePolicies policies) {
        this.attempts = Objects.requireNonNull(attempts);
        this.policy = Objects.requireNonNull(policies).reconfirmation();
    }

    public <R> R confirm(long userId, Supplier<R> action) {
        String subject = Long.toString(userId);
        AttemptOutcome outcome = attempts.hit(policy, subject);
        if (!outcome.allowed()) {
            throw new TooManyAttemptsException(outcome.retryAfter());
        }
        R result;
        try {
            result = action.get();
        } catch (InvalidCredentialsException | AuthenticationFailedException wrongPassword) {
            throw wrongPassword;
        } catch (RuntimeException accepted) {
            forget(subject);
            throw accepted;
        }
        forget(subject);
        return result;
    }

    /**
     * A count that could not be cleared only makes the person wait a little longer; failing here would undo an action
     * whose password was already accepted.
     */
    private void forget(String subject) {
        try {
            attempts.clear(policy, subject);
        } catch (DependencyUnavailableException ignored) {
            // the count expires with its window
        }
    }
}
