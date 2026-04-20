package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.exception.CompromisedSessionException;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.exception.InvalidRefreshTokenException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.UserId;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class Session {

    private static final Duration SESSION_LIFESPAN = Duration.ofDays(7);
    private static final Duration REFRESH_GRACE_PERIOD = Duration.ofSeconds(30);

    @NonNull
    private final SessionId sessionId;

    @NonNull
    private final UserId userId;

    @NonNull
    private final DeviceInfo deviceInfo;
    
    private int generation;

    @NonNull
    private Instant issuedAt;

    private Instant lastRefreshedAt;

    @NonNull
    private Instant expiresAt;

    private boolean revoked;
    private Instant revokedAt;

    private final Integer version;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();



    private Session(
            @NonNull SessionId sessionId,
            @NonNull UserId userId,
            @NonNull DeviceInfo deviceInfo,
            int generation,
            @NonNull Instant issuedAt,
            Instant lastRefreshedAt,
            @NonNull Instant expiresAt,
            boolean revoked,
            Instant revokedAt,
            Integer version) {

        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceInfo = deviceInfo;
        this.generation = generation;
        this.issuedAt = issuedAt;
        this.lastRefreshedAt = lastRefreshedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.version = version;
    }

    public static Session reconstitute(
                @NonNull SessionId sessionId,
                @NonNull UserId userId,
                @NonNull DeviceInfo deviceInfo,
                int generation,
                @NonNull Instant issuedAt,
                Instant lastRefreshedAt,
                @NonNull Instant expiresAt,
                boolean revoked,
                Instant revokedAt,
                Integer version) {

        return new Session(
            sessionId,
            userId,
            deviceInfo,
            generation,
            issuedAt, 
            lastRefreshedAt, 
            expiresAt,
            revoked, 
            revokedAt,
            version);
    }


    /*
    * STATIC FACTORIES
    */
    public static Session createNew(
                @NonNull SessionId sessionId,
                @NonNull UserId userId,
                @NonNull DeviceInfo deviceInfo,
                Duration duration) {

        Instant now = Instant.now();
        Duration dur = Objects.requireNonNullElse(duration, SESSION_LIFESPAN);
        Session session = new Session(
            sessionId,
            userId,
            deviceInfo,
            1,
            now,
            null,
            now.plus(dur),
            false,
            null,
            null
        );

        return session;
    }


    public boolean refresh(int incomingGeneration) {
        if (this.revoked) {
            throw new InvalidRefreshTokenException("Session is completely revoked at " + this.revokedAt);
        }

        Instant now = Instant.now();
        if (now.isAfter(this.expiresAt)) {
            throw new InvalidRefreshTokenException("Token is expired at " + this.expiresAt);
        }

        if (incomingGeneration > this.generation) {
            throw new InvalidRefreshTokenException("Invalid token generation: Future generation detected.");
        }

        if (incomingGeneration == this.generation) {
            this.generation = this.generation + 1;
            this.lastRefreshedAt = now;
            this.expiresAt = now.plus(SESSION_LIFESPAN);
            return true; 
        }

        if (incomingGeneration == this.generation - 1) {
            if (this.lastRefreshedAt != null) {
                Instant gracePeriodEnd = this.lastRefreshedAt.plus(REFRESH_GRACE_PERIOD);
                
                if (!now.isAfter(gracePeriodEnd)) {
                    return false;
                }
            }
            
            markAsCompromised();
            throw new CompromisedSessionException("Reuse Detection: Grace period exceeded for previous generation.");
        }

        markAsCompromised();
        throw new CompromisedSessionException("Reuse Detection: Old token generation used. Session compromised.");
    }


    public boolean revoke() {
        if (this.revoked) {
            return false; 
        }

        if (Instant.now().isAfter(this.expiresAt)) {
            return false; 
        }
        
        this.revoked = true;
        this.revokedAt = Instant.now();
        return true;
    }


    public boolean logoutFromCurrentSession(SessionId incomingSessionId) {
        if (!this.sessionId.equals(incomingSessionId)) {
            throw new BusinessRuleViolationException("Session ID mismatch: Cannot logout from a different session");
        }
        
        return revoke();
    }


    private void markAsCompromised() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
