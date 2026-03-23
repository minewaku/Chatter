package com.minewaku.chatter.identityaccess.domain.sharedkernel.value;

import java.time.Instant;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.StateAlreadySatisfiedException;

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
            throw new StateAlreadySatisfiedException("User is already soft deleted");
        }

        return new DeletionStatus(true, Instant.now());
    }

    public DeletionStatus markRestored() {
        if (!this.deleted) {
            throw new StateAlreadySatisfiedException("User is not soft deleted");
        }
        return new DeletionStatus(false, null);
    }
}