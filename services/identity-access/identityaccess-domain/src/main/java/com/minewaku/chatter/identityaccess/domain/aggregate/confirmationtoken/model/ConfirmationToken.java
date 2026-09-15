package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.AggregateRoot;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class ConfirmationToken extends AggregateRoot<ConfirmationTokenId> {

    private final ConfirmationTokenId id;

    private final UserId userId;

    private final Email email;

    private final Duration duration;

    private final Instant createdAt;

    private final Instant expiresAt;

    private Instant confirmedAt;

    private final List<DomainEvent> events = new ArrayList<>();

    private ConfirmationToken(
            @NonNull ConfirmationTokenId id,
            @NonNull UserId userId,
            @NonNull Email email,
            Duration duration,
            Instant createdAt,
            Instant expiresAt,
            Instant confirmedAt) {

        this.id = id;
        this.userId = userId;
        this.email = email;
        this.duration = duration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
    }

    public static ConfirmationToken createNew(
            @NonNull ConfirmationTokenId id,
            @NonNull UserId userId,
            @NonNull Email email,
            Duration duration) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(email, "email cannot be null");

        Instant now = Instant.now();
        Duration dur = Objects.requireNonNullElse(duration, Duration.ofMinutes(15L));
        return new ConfirmationToken(id, userId, email, dur, now, now.plus(dur), null);
    }

    public static ConfirmationToken reconstitute(
            @NonNull ConfirmationTokenId id,
            @NonNull UserId userId,
            @NonNull Email email,
            Duration duration,
            Instant createdAt,
            Instant expiresAt,
            Instant confirmedAt) {
        return new ConfirmationToken(id, userId, email, duration, createdAt, expiresAt, confirmedAt);
    }

    @Override
    public ConfirmationTokenId getId() {
        return this.id;
    }

    public void verifyToken() {
        if (this.confirmedAt != null) {
            throw new BusinessRuleViolationException("Token already confirmed");
        }
        if (Instant.now().isAfter(this.expiresAt)) {
            throw new BusinessRuleViolationException("Token expired");
        }
        this.confirmedAt = Instant.now();

        ConfirmationTokenVerifiedDomainEvent event = new ConfirmationTokenVerifiedDomainEvent(
                String.valueOf(this.userId.getValue()),
                id.getValue());

        this.events.add(event);
    }
}
