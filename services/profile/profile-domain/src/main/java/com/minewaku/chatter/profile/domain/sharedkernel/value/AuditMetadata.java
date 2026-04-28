package com.minewaku.chatter.profile.domain.sharedkernel.value;

import java.time.Instant;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.InsertOnlyProperty;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class AuditMetadata {
	
    @Column("created_at")
    @InsertOnlyProperty
    private final Instant createdAt;

    @Column("modified_at")
    private Instant modifiedAt;

    @PersistenceCreator
    public AuditMetadata(
    		@NonNull Instant createdAt, 
    		Instant modifiedAt) {
    	
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static AuditMetadata createNew() {
        Instant now = Instant.now();
        return new AuditMetadata(now, now);
    }

    public AuditMetadata markUpdated() {
        Instant now = Instant.now();
        if (now.isBefore(this.createdAt)) {
            throw new DomainValidationException("Modified date cannot be before the created date");
        }
        
        return new AuditMetadata(this.createdAt, now);
    }
}
