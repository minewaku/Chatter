package com.minewaku.chatter.domain.value;

import java.time.Instant;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record AuditMetadata(@NonNull Instant createdAt, Instant modifiedAt) {

    public AuditMetadata() {
        this(Instant.now(), null);
    }
    
    public AuditMetadata {
        if (modifiedAt != null && modifiedAt.isBefore(createdAt)) {
             throw new DomainValidationException("Modified date cannot be before createdAt");
        }
    }

    public static AuditMetadata of(Instant createdAt, Instant modifiedAt) {
        return createdAt == null && modifiedAt == null ? null : new AuditMetadata(createdAt, modifiedAt);
    }

    public AuditMetadata markUpdated() {
        Instant now = Instant.now();
        return new AuditMetadata(this.createdAt, now); 
    }
}
