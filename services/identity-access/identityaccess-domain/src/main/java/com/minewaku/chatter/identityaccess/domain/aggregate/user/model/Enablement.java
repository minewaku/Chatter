package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.UserNotAccessibleException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.DeletionStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Enablement {
    
    private boolean enabled;
    private boolean locked;

    @NonNull
    private DeletionStatus deletionStatus;

    public Enablement() {
        this.enabled = false;
        this.locked = false;
        this.deletionStatus = new DeletionStatus();
    }

    public Enablement(boolean enabled, boolean locked, DeletionStatus deletionStatus) {
        this.enabled = enabled;
        this.locked = locked;
        this.deletionStatus = deletionStatus;
    }

    public void validateAccessible() {
        if (this.deletionStatus.isDeleted()) {
            throw new UserNotAccessibleException("This user has been soft deleted");
        }
        if (this.locked) {
            throw new UserNotAccessibleException("User is locked");
        }
        if (!this.enabled) {
            throw new UserNotAccessibleException("User is disabled");
        }
    }

    public boolean isUnverified() {
        return !this.enabled && !this.locked && !this.deletionStatus.isDeleted();
    }

    public boolean isBanned() {
        return !this.enabled && this.locked && !this.deletionStatus.isDeleted();
    }

    public boolean isSoftDeleted() {
        return this.deletionStatus.isDeleted();
    }

    protected Enablement lock() {
        if (this.locked) return this; 
        return new Enablement(this.enabled, true, this.deletionStatus);
    }

    protected Enablement unlock() {
        if (!this.locked) return this;
        return new Enablement(this.enabled, false, this.deletionStatus);
    }

    protected Enablement enable() {
        if (this.enabled) return this;
        return new Enablement(true, this.locked, this.deletionStatus);
    }
    
    protected Enablement disable() {
        if (!this.enabled) return this;
        return new Enablement(false, this.locked, this.deletionStatus);
    }

    protected Enablement softDelete() {
        DeletionStatus newDeletionStatus = this.deletionStatus.markDeleted();

        if (!this.enabled && this.deletionStatus.equals(newDeletionStatus)) {
            return this;
        }

        return new Enablement(false, this.locked, newDeletionStatus);
    }
}