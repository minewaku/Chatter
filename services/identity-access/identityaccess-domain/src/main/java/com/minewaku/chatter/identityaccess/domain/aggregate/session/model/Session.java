package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionRefreshedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.CompromisedSessionException;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.InvalidRefreshTokenException;
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


    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();



    private Session(
            @NonNull SessionId sessionId,
            @NonNull UserId userId,
            @NonNull DeviceInfo deviceInfo,
            int generation,
            @NonNull Instant issuedAt,
            @NonNull Instant lastRefreshedAt,
            @NonNull Instant expiresAt,
            boolean revoked,
            Instant revokedAt) {

        this.sessionId = sessionId;
        this.userId = userId;
        this.deviceInfo = deviceInfo;
        this.generation = generation;
        this.issuedAt = issuedAt;
        this.lastRefreshedAt = lastRefreshedAt;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    public static Session reconstitute(
                @NonNull SessionId sessionId,
                @NonNull UserId userId,
                @NonNull DeviceInfo deviceInfo,
                int generation,
                @NonNull Instant issuedAt,
                @NonNull Instant lastRefreshedAt,
                @NonNull Instant expiresAt,
                boolean revoked,
                Instant revokedAt) {

        return new Session(
            sessionId,
            userId,
            deviceInfo,
            generation,
            issuedAt, 
            lastRefreshedAt, 
            expiresAt,
            revoked, 
            revokedAt);
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
            null
        );

        SessionCreatedDomainEvent event = new SessionCreatedDomainEvent(sessionId.getValue().toString(), userId.getValue().toString(), now.toString());
        session.events.add(event);

        return session;
    }


    // RECHECK: recheck the logic and exception types
    public void refresh(int incomingGeneration) {

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
            
            SessionRefreshedDomainEvent event = new SessionRefreshedDomainEvent(
                this.sessionId.getValue().toString(),
                this.userId.getValue().toString(),
                now.toString()
            );
            this.events.add(event);
            
            return;
        }

        if (incomingGeneration == this.generation - 1) {
            if (this.lastRefreshedAt != null) {
                Instant gracePeriodEnd = this.lastRefreshedAt.plus(REFRESH_GRACE_PERIOD);
                
                if (!now.isAfter(gracePeriodEnd)) {
                    return;
                }
            }
            
            markAsCompromised();
            throw new CompromisedSessionException("Reuse Detection: Grace period exceeded for previous generation.");
        }

        markAsCompromised();
        throw new CompromisedSessionException("Reuse Detection: Old token generation used. Session compromised.");
    }

    private void markAsCompromised() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    public void revoke() {
        if (revoked) {
            throw new BusinessRuleViolationException("Session already revoked at " + revokedAt);
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new BusinessRuleViolationException("Cannot revoke an expired session");
        }
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    public void logoutFromCurrentSession(SessionId incomingSessionId) {
        if (!this.sessionId.equals(incomingSessionId)) {
            throw new BusinessRuleViolationException("Session ID mismatch: Cannot logout from a different session");
        }
        revoke();
    }
}
