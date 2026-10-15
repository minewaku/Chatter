package com.minewaku.chatter.identityaccess.infrastructure.support;

import com.minewaku.chatter.identityaccess.emailverification.internal.model.EmailVerification;
import com.minewaku.chatter.identityaccess.user.internal.model.AccountStatus;
import com.minewaku.chatter.identityaccess.user.internal.model.Birthday;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.model.PasswordHash;
import com.minewaku.chatter.identityaccess.user.internal.model.UserAccount;
import com.minewaku.chatter.identityaccess.user.internal.model.UserId;
import com.minewaku.chatter.identityaccess.user.internal.model.Username;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Builds valid core models for adapter tests. Instants use microsecond precision (data-model conventions). */
public final class AccountFixtures {

    public static final Instant NOW = Instant.parse("2026-10-15T10:15:30.123456Z");

    private AccountFixtures() {}

    public static UserAccount pendingAccount(long id, String email, String username) {
        return UserAccount.register(
                new UserId(id),
                new Email(email),
                new Username(username),
                new Birthday(LocalDate.of(1999, 1, 1)),
                new PasswordHash("argon2id", "$argon2id$v=19$m=65536,t=3,p=1$c2FsdA$aGFzaA", new byte[] {1, 2, 3, 4}));
    }

    public static UserAccount reconstitutedAccount(long id, AccountStatus status, Instant deletedAt) {
        return UserAccount.reconstitute(
                new UserId(id),
                new Email("reconstituted" + id + "@example.com"),
                new Username("user" + id),
                new Birthday(LocalDate.of(1990, 6, 15)),
                status,
                deletedAt,
                new PasswordHash("argon2id", "$argon2id$hash", new byte[] {9, 8, 7}),
                NOW.truncatedTo(ChronoUnit.MICROS),
                NOW.plusSeconds(5).truncatedTo(ChronoUnit.MICROS),
                0L);
    }

    public static EmailVerification verification(long userId, String rawToken) {
        return EmailVerification.issue(
                rawToken,
                new UserId(userId),
                new Email("verify" + userId + "@example.com"),
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC),
                Duration.ofHours(24));
    }
}
