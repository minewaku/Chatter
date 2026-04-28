package  com.minewaku.chatter.profile.domain.model.profile.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;

import com.minewaku.chatter.profile.domain.model.profile.exception.UserNotAccessibleException;
import com.minewaku.chatter.profile.domain.model.profile.exception.UserSoftDeletedException;
import com.minewaku.chatter.profile.domain.sharedkernel.value.DeletionStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Enablement {
    
    @Column("is_enabled")
    private Boolean enabled;

    @Column("is_locked")
    private Boolean locked;

    @Embedded.Nullable
    private DeletionStatus deletionStatus;

    @PersistenceCreator
    public Enablement(
                @NonNull Boolean enabled, 
                @NonNull Boolean locked, 
                @NonNull DeletionStatus deletionStatus) {

        this.enabled = enabled != null ? enabled : false;
        this.locked = locked != null ? locked : false;
        this.deletionStatus = deletionStatus;
    }

    public static Enablement createNew() {
        return new Enablement(false, false, DeletionStatus.createNew());
    }

    public Enablement enabled() {
        return new Enablement(true, this.locked, this.deletionStatus);
    }

    public void validateAccessible() {
        if (this.deletionStatus.getDeleted()) {
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
        return !this.enabled && !this.locked && !this.deletionStatus.getDeleted();
    }

    public boolean isBanned() {
        return !this.enabled && this.locked && !this.deletionStatus.getDeleted();
    }

    public boolean isSoftDeleted() {
        return this.deletionStatus.getDeleted();
    }
}
