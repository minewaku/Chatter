package com.minewaku.chatter.identityaccess.user.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.authentication.api.exception.AuthenticationFailedException;
import com.minewaku.chatter.identityaccess.shared.exception.DependencyUnavailableException;
import com.minewaku.chatter.identityaccess.shared.exception.TooManyAttemptsException;
import com.minewaku.chatter.identityaccess.shared.throttle.InMemoryAttemptLimiter;
import com.minewaku.chatter.identityaccess.shared.throttle.TestPolicies;
import com.minewaku.chatter.identityaccess.user.api.exception.DuplicateUserAccountException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ReauthenticationGuardTest {

    private final InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter();
    private final ReauthenticationGuard guard = new ReauthenticationGuard(limiter, TestPolicies.defaults());
    private final AtomicInteger runs = new AtomicInteger();

    private void wrongPassword(long userId) {
        assertThrows(
                InvalidCredentialsException.class,
                () -> guard.confirm(userId, () -> {
                    runs.incrementAndGet();
                    throw new InvalidCredentialsException("wrong");
                }));
    }

    @Test
    void runsTheActionAndReturnsItsResultWhenAllowed() {
        assertEquals("done", guard.confirm(42, () -> "done"));
    }

    @Test
    void aWrongPasswordIsRethrownAndKeepsTheCount() {
        wrongPassword(42);

        assertEquals(1, limiter.count(TestPolicies.defaults().reconfirmation(), "42"));
    }

    @Test
    void aFailedAuthenticationFromTheSessionCommandsAlsoKeepsTheCount() {
        assertThrows(
                AuthenticationFailedException.class,
                () -> guard.confirm(42, () -> {
                    throw new AuthenticationFailedException();
                }));

        assertEquals(1, limiter.count(TestPolicies.defaults().reconfirmation(), "42"));
    }

    @Test
    void aCorrectPasswordClearsTheCount() {
        wrongPassword(42);
        wrongPassword(42);

        guard.confirm(42, () -> "done");

        assertEquals(0, limiter.count(TestPolicies.defaults().reconfirmation(), "42"));
    }

    @Test
    void aConflictReachedAfterThePasswordWasAcceptedClearsTheCountAndIsRethrown() {
        wrongPassword(42);
        DuplicateUserAccountException conflict = new DuplicateUserAccountException("taken");

        DuplicateUserAccountException thrown = assertThrows(
                DuplicateUserAccountException.class,
                () -> guard.confirm(42, () -> {
                    throw conflict;
                }));

        assertSame(conflict, thrown);
        assertEquals(0, limiter.count(TestPolicies.defaults().reconfirmation(), "42"));
    }

    @Test
    void afterFiveWrongPasswordsTheActionIsNotRunAtAllEvenForTheRightPassword() {
        for (int i = 0; i < 5; i++) {
            wrongPassword(42);
        }
        int before = runs.get();

        TooManyAttemptsException refused = assertThrows(
                TooManyAttemptsException.class,
                () -> guard.confirm(42, () -> {
                    runs.incrementAndGet();
                    return "would have been the right password";
                }));

        assertEquals(before, runs.get());
        assertTrue(refused.retryAfter().toMillis() > 0);
    }

    @Test
    void peopleAreCountedSeparately() {
        for (int i = 0; i < 5; i++) {
            wrongPassword(42);
        }

        assertEquals("done", guard.confirm(43, () -> "done"));
    }

    @Test
    void aNewWindowLetsThePersonConfirmAgain() {
        for (int i = 0; i < 5; i++) {
            wrongPassword(42);
        }
        limiter.expire();

        assertEquals("done", guard.confirm(42, () -> "done"));
    }

    @Test
    void anUnavailableLimiterStopsTheActionInsteadOfLeavingItUnlimited() {
        limiter.setUnavailable(true);

        assertThrows(
                DependencyUnavailableException.class,
                () -> guard.confirm(42, () -> {
                    runs.incrementAndGet();
                    return "done";
                }));
        assertEquals(0, runs.get());
    }

    @Test
    void aCountThatCannotBeClearedDoesNotUndoAnAcceptedAction() {
        InMemoryAttemptLimiter flaky = new InMemoryAttemptLimiter() {
            @Override
            public void clear(
                    com.minewaku.chatter.identityaccess.shared.throttle.AttemptPolicy policy, String subject) {
                throw new DependencyUnavailableException(java.time.Duration.ofSeconds(1), null);
            }
        };
        ReauthenticationGuard tolerant = new ReauthenticationGuard(flaky, TestPolicies.defaults());

        assertEquals("done", tolerant.confirm(42, () -> "done"));
    }
}
