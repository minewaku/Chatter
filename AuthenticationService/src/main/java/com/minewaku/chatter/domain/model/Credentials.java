package com.minewaku.chatter.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class Credentials {

    private static final long PASSWORD_CHANGE_MIN_INTERVAL_MINUTES = 5L;

    @NonNull
    private final UserId userId;

    @NonNull
    private HashedPassword hashedPassword;

    private Instant modifiedAt;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private Credentials(
            @NonNull UserId userId,
            @NonNull HashedPassword hashedPassword,
            Instant modifiedAt) {

        this.userId = userId;
        this.hashedPassword = hashedPassword;
        this.modifiedAt = modifiedAt;
    }


    /*
    * STATIC FACTORIES
    */
    static public Credentials createNew(
            @NonNull UserId userId,
            @NonNull HashedPassword hashedPassword) {
                
        Credentials credentials = new Credentials(userId, hashedPassword, null);
        CreateConfirmationTokenDomainEvent createConfirmationTokenDomainEvent = 
            new CreateConfirmationTokenDomainEvent(
                userId, null
            );

        credentials.events.add(createConfirmationTokenDomainEvent);
        return credentials;
    }

    public static Credentials reconstitute(
                @NonNull UserId userId, 
                @NonNull HashedPassword hashedPassword, 
                Instant modifiedAt) {
        return new Credentials(userId, hashedPassword, modifiedAt);
    }


    public void changePassword(@NonNull HashedPassword newHashedPassword) {
        
        if (this.modifiedAt != null) {
            Instant nextAllowedTime = this.modifiedAt.plus(PASSWORD_CHANGE_MIN_INTERVAL_MINUTES, ChronoUnit.MINUTES);
            Instant now = Instant.now();
            if (now.isBefore(nextAllowedTime)) {
                throw new BusinessRuleViolationException(
                    String.format("you only allow to change password once every %d minutes", PASSWORD_CHANGE_MIN_INTERVAL_MINUTES)
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
