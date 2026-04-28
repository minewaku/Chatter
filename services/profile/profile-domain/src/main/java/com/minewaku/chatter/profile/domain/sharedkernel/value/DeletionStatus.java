package com.minewaku.chatter.profile.domain.sharedkernel.value;

import java.time.Instant;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class DeletionStatus {
    
    @Column("is_deleted")
    private Boolean deleted;

    @Column("deleted_at")
    private Instant deletedAt;

    @PersistenceCreator
    public DeletionStatus(
                @NonNull Boolean deleted, 
                Instant deletedAt) {

        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    public static DeletionStatus createNew() {
        return new DeletionStatus(false, null);
    }

    public DeletionStatus markDeleted() {
        if (this.deleted) {
            return this;
        }
        return new DeletionStatus(true, Instant.now());
    }

    public DeletionStatus markRestored() {
        if (!this.deleted) {
            return this;
        }
        return new DeletionStatus(false, null);
    }
}