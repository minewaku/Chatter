package com.minewaku.chatter.domain.value;

import java.time.Instant;

import com.minewaku.chatter.domain.exception.DomainValidationException;

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

    public void markUpdated() {
        Instant now = Instant.now();
        if (now.isBefore(this.createdAt)) {
            throw new DomainValidationException("Modified date cannot be before the created date");
        }
        
        this.modifiedAt = now;
    }
}
