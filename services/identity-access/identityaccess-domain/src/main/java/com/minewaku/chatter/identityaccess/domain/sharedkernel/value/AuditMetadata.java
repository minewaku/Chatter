package com.minewaku.chatter.identityaccess.domain.sharedkernel.value;

import java.time.Instant;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class AuditMetadata {
	
    @NonNull
    private final Instant createdAt;

    private Instant modifiedAt;

    public AuditMetadata() {
        this.createdAt = Instant.now();
        this.modifiedAt = null;
    }

    public AuditMetadata(
    		@NonNull Instant createdAt, 
    		Instant modifiedAt) {
    	
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public AuditMetadata markUpdated() {
        Instant now = Instant.now();
        if (now.isBefore(this.createdAt)) {
            throw new DomainValidationException("Modified date cannot be before the created date");
        }
        
        return new AuditMetadata(this.createdAt, now);
    }
}
