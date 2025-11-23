package com.minewaku.chatter.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.MailType;
import com.minewaku.chatter.domain.value.id.UserId;

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
    private ConfirmationToken(@NonNull String token, @NonNull UserId userId, @NonNull Email email, Duration duration, Instant createdAt, Instant expiresAt, Instant confirmedAt) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.duration = Objects.isNull(duration) ? Duration.ofMinutes(15L) : duration;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
    }

    // Static factory for loading existing data
    public static ConfirmationToken reconstitute(@NonNull String token, @NonNull UserId userId, @NonNull Email email, Duration duration, Instant createdAt, Instant expiresAt, Instant confirmedAt) {
        return new ConfirmationToken(token, userId, email, duration, createdAt, expiresAt, confirmedAt);
    }

    
    // Static factory for creating new data
    public static ConfirmationToken createNew(@NonNull String token, @NonNull UserId userId, @NonNull Email email, Duration duration) {
        Instant now = Instant.now();
        Duration dur = Objects.isNull(duration) ? Duration.ofMinutes(15L) : duration;
        
        ConfirmationToken confirmationToken = new ConfirmationToken(token, userId, email, dur, now, now.plus(dur), null);
        
        SendConfirmationTokenDomainEvent sendConfirmationTokenDomainEvent = new SendConfirmationTokenDomainEvent(confirmationToken, MailType.EMAIL_CONFIRMATION, "Chatter Email Confirmation");
        confirmationToken.events.add(sendConfirmationTokenDomainEvent);
        
        return confirmationToken;
    }
    
    
    
    public void verifyToken() {
        if (this.confirmedAt != null) {
            throw new IllegalStateException("Token already confirmed");
        }
        if (Instant.now().isAfter(this.expiresAt)) {
            throw new IllegalStateException("Token expired");
        }
        this.confirmedAt = Instant.now();
    }

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfirmationToken)) return false;
        ConfirmationToken that = (ConfirmationToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
