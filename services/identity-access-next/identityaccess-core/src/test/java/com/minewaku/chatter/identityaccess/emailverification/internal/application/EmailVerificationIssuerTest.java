package com.minewaku.chatter.identityaccess.emailverification.internal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.emailverification.internal.model.VerificationPurpose;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EmailVerificationIssuerTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UserId USER = new UserId(1);
    private static final Email EMAIL = new Email("person@example.com");

    private final EmailVerificationTestFakes.Repository repository = new EmailVerificationTestFakes.Repository();
    private final EmailVerificationTestFakes.Generator generator = new EmailVerificationTestFakes.Generator();
    private final EmailVerificationIssuer issuer =
            new EmailVerificationIssuer(Clock.fixed(NOW, ZoneOffset.UTC), generator, repository);

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void every_purpose_gets_the_same_twenty_four_hour_lifetime(VerificationPurpose purpose) {
        EmailVerification issued = issuer.issue(purpose, USER, EMAIL);

        assertEquals(purpose, issued.purpose());
        assertEquals(Duration.ofHours(24), EmailVerificationIssuer.LIFETIME);
        assertEquals(NOW.plus(EmailVerificationIssuer.LIFETIME), issued.expiresAt());
        assertEquals(issued, repository.records.get("token-1"));
    }

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void issuing_again_replaces_the_earlier_proof_of_that_purpose(VerificationPurpose purpose) {
        issuer.issue(purpose, USER, EMAIL);
        issuer.issue(purpose, USER, EMAIL);

        assertEquals(List.of("invalidate", "save", "invalidate", "save"), repository.calls);
        assertTrue(repository.findByPresentedToken(purpose, "token-1").isEmpty());
        assertTrue(repository.findByPresentedToken(purpose, "token-2").isPresent());
    }

    @Test
    void purposes_are_independent_for_the_same_user() {
        issuer.issue(VerificationPurpose.REGISTRATION, USER, EMAIL);
        issuer.issue(VerificationPurpose.EMAIL_CHANGE, USER, new Email("new@example.com"));

        assertTrue(
                repository.outstandingFor(VerificationPurpose.REGISTRATION, 1).isPresent());
        assertTrue(
                repository.outstandingFor(VerificationPurpose.EMAIL_CHANGE, 1).isPresent());

        issuer.issue(VerificationPurpose.EMAIL_CHANGE, USER, new Email("newer@example.com"));

        assertEquals(
                "token-1",
                repository
                        .outstandingFor(VerificationPurpose.REGISTRATION, 1)
                        .orElseThrow()
                        .rawToken());
        assertFalse(repository
                .findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, "token-2")
                .isPresent());
    }

    @Test
    void a_token_is_not_found_under_another_purpose() {
        issuer.issue(VerificationPurpose.REGISTRATION, USER, EMAIL);

        assertTrue(repository
                .findByPresentedToken(VerificationPurpose.EMAIL_CHANGE, "token-1")
                .isEmpty());
    }

    @Test
    void rejects_a_non_positive_lifetime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmailVerificationIssuer(Clock.systemUTC(), Duration.ZERO, generator, repository));
    }
}
