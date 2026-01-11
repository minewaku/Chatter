package com.minewaku.chatter.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.UserRestoredDomainEvent;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.dto.CreateUserDto;
import com.minewaku.chatter.domain.exception.StateAlreadySatisfiedException;
import com.minewaku.chatter.domain.exception.UserNotAccessibleException;
import com.minewaku.chatter.domain.exception.UserSoftDeletedException;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class User {

    @NonNull
    private final UserId id;

    @NonNull
    private final Email email;

    @NonNull
    private final Username username;

    @NonNull
    private final Birthday birthday;

    @Setter
    private boolean isEnabled;

    @Setter
    private boolean isLocked;

    @Setter
    private boolean isDeleted;

    @NonNull
    private final AuditMetadata auditMetadata;

    private Instant deletedAt;

    @NonNull
    @JsonIgnore
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private User(@NonNull UserId id, @NonNull Email email,
            @NonNull Username username, @NonNull Birthday birthday, @NonNull AuditMetadata auditMetadata,
            boolean isEnabled, boolean isLocked, boolean isDeleted, Instant deletedAt) {

        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.isEnabled = isEnabled;
        this.isLocked = isLocked;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.auditMetadata = auditMetadata;
    }

    // Static factory for loading existing data
    public static User reconstitute(@NonNull UserId id, @NonNull Email email,
            @NonNull Username username, @NonNull Birthday birthday,
            @NonNull AuditMetadata auditMetadata,
            boolean isEnabled, boolean isLocked, boolean isDeleted, Instant deletedAt) {

        return new User(id, email, username, birthday, auditMetadata, isEnabled, isLocked, isDeleted, deletedAt);
    }

    // Static factory for creating new data
    public static User createNew(@NonNull UserId id, @NonNull Email email, @NonNull Username username,
            @NonNull Birthday birthday) {
        User user = new User(id, email, username, birthday, new AuditMetadata(), false, false, false, null);
        CreateUserDto createdUserDto = new CreateUserDto(id, email, username, birthday, user.getAuditMetadata());

        UserCreatedDomainEvent userCreatedDomainEvent = new UserCreatedDomainEvent(createdUserDto);
        user.getEvents().add(userCreatedDomainEvent);
        return user;
    }

    public void reRegisterWithoutVerifyFirst(UserId userId) {
        CreateConfirmationTokenDomainEvent createConfirmationTokenDomainEvent = new CreateConfirmationTokenDomainEvent(
                userId, null);
        this.getEvents().add(createConfirmationTokenDomainEvent);
    }

    public void validateAccessible() {
        if (this.isDeleted) {
            throw new UserSoftDeletedException("This user has been soft deleted");
        }
        if (this.isLocked) {
            throw new UserNotAccessibleException("User is locked");
        }
        if (!this.isEnabled) {
            throw new UserNotAccessibleException("User is unabled");
        }
    }

    public void checkForDisable() {
        if (this.isEnabled) {
            throw new UserNotAccessibleException("User is already enabled");
        }
    }

    public void checkForSoftDeleted() {
        if (this.isDeleted) {
            throw new UserNotAccessibleException("User is already soft deleted");
        }
    }

    public void checkForLocked() {
        if (this.isLocked) {
            throw new UserNotAccessibleException("User is already locked");
        }
    }

    public void hardDeleted() {
        UserHardDeletedDomainEvent userHardDeletedDomainEvent = new UserHardDeletedDomainEvent(this.id);
        this.getEvents().add(userHardDeletedDomainEvent);
    }

    public void softDelete() {
        if(this.isDeleted) {
            throw new StateAlreadySatisfiedException("User is already soft deleted");
        }
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        UserSoftDeletedDomainEvent userSoftDeletedDomainEvent = new UserSoftDeletedDomainEvent(this.id);
        this.events.add(userSoftDeletedDomainEvent);
    }

    public void restore() {
        if(!this.isDeleted) {
            throw new StateAlreadySatisfiedException("User is not soft deleted");
        }
        this.isDeleted = false;
        this.deletedAt = null;
        UserRestoredDomainEvent userRestoredDomainEvent = new UserRestoredDomainEvent(this.id);
        this.events.add(userRestoredDomainEvent);
    }

    public void enable() {
        if(this.isEnabled) {
            throw new StateAlreadySatisfiedException("User is already enabled");
        }
        this.isEnabled = true;
        this.auditMetadata.markUpdated();
        AccountVerifiedDomainEvent accountVerifiedDomainEvent = new AccountVerifiedDomainEvent(this.id);
        this.events.add(accountVerifiedDomainEvent);
    }

    public void lock() {
        if(this.isLocked) {
            throw new StateAlreadySatisfiedException("User is already locked");
        }
        this.isLocked = true;
        this.auditMetadata.markUpdated();
        UserLockedDomainEvent lockUserDomainEvent = new UserLockedDomainEvent(this.id);
        this.events.add(lockUserDomainEvent);
    }

    public void unlock() {
        if(!this.isLocked) {
            throw new StateAlreadySatisfiedException("User is already unlocked");
        }
        this.isLocked = false;
        this.auditMetadata.markUpdated();
        UserUnlockedDomainEvent unlockUserDomainEvent = new UserUnlockedDomainEvent(this.id);
        this.events.add(unlockUserDomainEvent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User other = (User) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
