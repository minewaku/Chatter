package com.minewaku.chatter.domain.value;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class AuditMetadata {
	
    @NonNull
    private final Instant createdAt;
    @Setter
    private Instant modifiedAt;

    // No-args constructor: for new objects
    public AuditMetadata() {
        this.createdAt = Instant.now();
        this.modifiedAt = null;
    }

    @JsonCreator
    // Full-args constructor: for loading from persistence
    public AuditMetadata(
    		@JsonProperty("createdAt") @NonNull Instant createdAt, 
    		@JsonProperty("modifiedAt") Instant modifiedAt) {
    	
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
