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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class ConfirmationToken {

    @NonNull
    private final String token;

    @NonNull
    private final UserId userId;

    @NonNull
    private final Email email;

    @NonNull
    private final Duration duration;

    @NonNull
    private final Instant createdAt;

    @NonNull
    private final Instant expiresAt;

    private Instant confirmedAt;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private ConfirmationToken(
            @NonNull String token,
            @NonNull UserId userId,
            @NonNull Email email,
            Duration duration,
            Instant createdAt,
            Instant expiresAt,
            Instant confirmedAt) {

        this.token = token;
        this.userId = userId;
        this.email = email;
        this.duration = duration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
    }


    /*
    * STATIC FACTORIES
    */
    public static ConfirmationToken reconstitute(
                @NonNull String token,
                @NonNull UserId userId,
                @NonNull Email email,
                Duration duration,
                Instant createdAt,
                Instant expiresAt,
                Instant confirmedAt) {

        return new ConfirmationToken(token, userId, email, duration, createdAt, expiresAt, confirmedAt);
    }

    public static ConfirmationToken createNew(
            @NonNull String token,
            @NonNull UserId userId,
            @NonNull Email email,
            Duration duration) {

        Instant now = Instant.now();
        Duration dur = Objects.requireNonNullElse(duration, Duration.ofMinutes(15L));

        ConfirmationToken confirmationToken = 
            new ConfirmationToken(
                token, userId, email, dur, now, now.plus(dur),null
            );

        return confirmationToken;
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
            token
        );

        this.events.add(event);
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConfirmationToken))
            return false;
        ConfirmationToken that = (ConfirmationToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
