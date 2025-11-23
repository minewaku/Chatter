package com.minewaku.chatter.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class RefreshToken {
	
	@NonNull
    private final String token;

	@NonNull
    private final Duration duration;

	@NonNull
    private final Instant issuedAt;

	@NonNull
    private final Instant expiresAt;

	@NonNull
    private final UserId userId;
    
    private boolean revoked = false;
    
    @NonNull
    private Instant revokedAt;
    
    @NonNull
    private String replacedBy;

    // Private constructor
    private RefreshToken(@NonNull String token, @NonNull Duration duration, 
            @NonNull Instant issuedAt, @NonNull UserId userId, 
            String replacedBy, boolean revoked, Instant revokedAt) {
    	
        if (replacedBy.isBlank()) {
            throw new IllegalArgumentException("ReplacedBy cannot be blank");
        }
        
        this.token = token;
        this.duration = duration;
        this.issuedAt = issuedAt;
        this.expiresAt = issuedAt.plus(duration);
        this.userId = userId;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    // Static factory for loading existing data
    public static RefreshToken reconstitute(@NonNull String token, @NonNull Duration duration, @NonNull Instant issuedAt, @NonNull UserId userId, String replacedBy, boolean revoked, Instant revokedAt) {
        return new RefreshToken(token, duration, issuedAt, userId, replacedBy, revoked, revokedAt);
    }

    // Static factory for loading existing data
    public static RefreshToken createNew(@NonNull String token, @NonNull Duration duration, @NonNull Instant issuedAt, @NonNull UserId userId) {
        return new RefreshToken(token, duration, issuedAt, userId, null, false, null);
    }

    public void replace(RefreshToken replacedBy) {
        revoke();
        if(token.equals(replacedBy.getToken())) {
            throw new IllegalStateException("New token cannot reuse old token value");
        }
        this.replacedBy = replacedBy.getToken();
    }

    public void revoke() {
        if (revoked) {
            throw new IllegalStateException("Token already revoked at " + revokedAt);
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Cannot revoke an expired token");
        }
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken)) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }
}
