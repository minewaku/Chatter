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

    public static Enablement enabled() {
        return new Enablement(true, false, new DeletionStatus());
    }


    public void validateAccessible() {
        if (this.deletionStatus.isDeleted()) {
            throw new UserSoftDeletedException("This user has been soft deleted");
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
}
