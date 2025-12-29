package com.minewaku.chatter.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class Credentials {

    @NonNull
    private final UserId userId;

    @NonNull
    @Setter
    private HashedPassword hashedPassword;

    @Setter
    private Instant modifiedAt;

    @NonNull
    @JsonIgnore
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    private Credentials(
            @NonNull UserId userId,
            @NonNull HashedPassword hashedPassword,
            Instant modifiedAt) {

        this.userId = userId;
        this.hashedPassword = hashedPassword;
        this.modifiedAt = modifiedAt;
    }

    static public Credentials createNew(
            @NonNull UserId userId,
            @NonNull HashedPassword hashedPassword) {
                
        Credentials credentials = new Credentials(userId, hashedPassword, null);
        CreateConfirmationTokenDomainEvent createConfirmationTokenDomainEvent = new CreateConfirmationTokenDomainEvent(
                userId, null);
        credentials.events.add(createConfirmationTokenDomainEvent);
        return credentials;
    }

    /**
     * Reconstitute an existing Credentials aggregate from persisted data without
     * emitting domain events (used by mappers/repositories).
     */
    public static Credentials reconstitute(@NonNull UserId userId, @NonNull HashedPassword hashedPassword, Instant modifiedAt) {
        return new Credentials(userId, hashedPassword, modifiedAt);
    }

    public void changePassword(@NonNull HashedPassword newHashedPassword) {
        if (this.modifiedAt != null) {
            Instant nextAllowedTime = this.modifiedAt.plus(5, ChronoUnit.MINUTES);
            Instant now = Instant.now();
            if (now.isBefore(nextAllowedTime)) {
                throw new BusinessRuleViolationException(
                    "you only allow to change password once every 5 minutes"
                );
            }
        }

        this.hashedPassword = newHashedPassword;
        this.modifiedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Credentials))
            return false;
        Credentials that = (Credentials) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "userId=" + userId +
                ", hashedPassword=[PROTECTED]" +
                '}';
    }
}
