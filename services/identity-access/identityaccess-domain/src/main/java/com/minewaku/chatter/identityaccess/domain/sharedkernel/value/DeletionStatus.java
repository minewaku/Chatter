package com.minewaku.chatter.identityaccess.domain.sharedkernel.value;

import java.time.Instant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class DeletionStatus {
    
    private boolean deleted;
    private Instant deletedAt;

    public DeletionStatus() {
        this.deleted = false;
        this.deletedAt = null;
    }

    public DeletionStatus(boolean deleted, Instant deletedAt) {
        this.deleted = deleted;
        this.deletedAt = deletedAt;
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