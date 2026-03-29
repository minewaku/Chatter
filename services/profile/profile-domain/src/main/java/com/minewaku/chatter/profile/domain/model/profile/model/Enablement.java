package  com.minewaku.chatter.profile.domain.model.profile.model;

import com.minewaku.chatter.profile.domain.model.profile.exception.UserNotAccessibleException;
import com.minewaku.chatter.profile.domain.model.profile.exception.UserSoftDeletedException;
import com.minewaku.chatter.profile.domain.sharedkernel.value.DeletionStatus;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Embeddable
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
            throw new UserSoftDeletedException("This user has been soft deleted");
        }
        if (this.locked) {
            throw new UserNotAccessibleException("User is locked");
        }
        if (!this.enabled) {
            throw new UserNotAccessibleException("User is unabled");
        }
    }

    public void checkForEnable() {
        if (this.enabled) {
            throw new UserNotAccessibleException("User is already enabled");
        }
    }

    public void checkForDisable() {
        if (!this.enabled) {
            throw new UserNotAccessibleException("User is already disabled");
        }
    }

    public void checkForSoftDeleted() {
        if (this.deletionStatus.isDeleted()) {
            throw new UserNotAccessibleException("User is already soft deleted");
        }
    }

    public void checkForLocked() {
        if (this.locked) {
            throw new UserNotAccessibleException("User is already locked");
        }
    }


    protected Enablement lock() {
        if (this.locked) {
            throw new UserNotAccessibleException("User is already locked");
        }

        return new Enablement(this.enabled, true, this.deletionStatus);
    }

    protected Enablement unlock() {
        if (!this.locked) {
            throw new UserNotAccessibleException("User is already unlocked");
        }

        return new Enablement(this.enabled, false, this.deletionStatus);
    }

    protected Enablement enable() {
        if (this.enabled) {
            throw new UserNotAccessibleException("User is already enabled");
        }

        return new Enablement(true, this.locked, this.deletionStatus);
    }

    
    protected Enablement disable() {
        if (!this.enabled) {
            throw new UserNotAccessibleException("User is already disabled");
        }

        return new Enablement(false, this.locked, this.deletionStatus);
    }


    protected Enablement softDelete() {
        DeletionStatus deletionStatus = this.deletionStatus.markDeleted();
        return new Enablement(this.enabled, this.locked, deletionStatus);
    }

    protected Enablement restore() {
        DeletionStatus deletionStatus = this.deletionStatus.markRestored();
        return new Enablement(this.enabled, this.locked, deletionStatus);
    }
}
