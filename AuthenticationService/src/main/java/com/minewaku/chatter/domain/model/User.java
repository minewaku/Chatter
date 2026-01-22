package com.minewaku.chatter.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.UserRestoredDomainEvent;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
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
    private boolean enabled;

    @Setter
    private boolean locked;

    @Setter
    private boolean deleted;

    @NonNull
    private final AuditMetadata auditMetadata;

    private Instant deletedAt;

    private Instant lastLoginAt;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private User(
                @NonNull UserId id, 
                @NonNull Email email,
                @NonNull Username username, 
                @NonNull Birthday birthday, 
                @NonNull AuditMetadata auditMetadata,
                boolean enabled, 
                boolean locked, 
                boolean deleted, 
                Instant deletedAt) {

        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.enabled = enabled;
        this.locked = locked;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.auditMetadata = auditMetadata;
    }


    /*
    * STATIC FACTORIES
    */
    public static User reconstitute(
                @NonNull UserId id, 
                @NonNull Email email,
                @NonNull Username username, 
                @NonNull Birthday birthday,
                @NonNull AuditMetadata auditMetadata,
                boolean enabled, 
                boolean locked, 
                boolean deleted, 
                Instant deletedAt) {

        return new User(id, email, username, birthday, auditMetadata, enabled, locked, deleted, deletedAt);
    }


    public static User createNew(
                @NonNull UserId id, 
                @NonNull Email email, 
                @NonNull Username username,
                @NonNull Birthday birthday) {

        User user = new User(id, email, username, birthday, new AuditMetadata(), false, false, false, null);
        UserCreatedDomainEvent userCreatedDomainEvent = new UserCreatedDomainEvent(
            id, email, username, birthday, user.getAuditMetadata()
        );

        user.getEvents().add(userCreatedDomainEvent);
        return user;
    }

    public void validateAccessible() {
        if (this.deleted) {
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
        if (this.deleted) {
            throw new UserNotAccessibleException("User is already soft deleted");
        }
    }

    public void checkForLocked() {
        if (this.locked) {
            throw new UserNotAccessibleException("User is already locked");
        }
    }


        
    public void reRegisterWithoutVerifyFirst(UserId userId) {
        CreateConfirmationTokenDomainEvent createConfirmationTokenDomainEvent = new CreateConfirmationTokenDomainEvent(
                userId, null);
        this.getEvents().add(createConfirmationTokenDomainEvent);
    }

    public void hardDeleted() {
        UserHardDeletedDomainEvent userHardDeletedDomainEvent = new UserHardDeletedDomainEvent(this.id);
        this.getEvents().add(userHardDeletedDomainEvent);
    }

    public void softDelete() {
        if(this.deleted) {
            throw new StateAlreadySatisfiedException("User is already soft deleted");
        }
        this.deleted = true;
        this.deletedAt = Instant.now();
        UserSoftDeletedDomainEvent userSoftDeletedDomainEvent = new UserSoftDeletedDomainEvent(this.id);
        this.events.add(userSoftDeletedDomainEvent);
    }

    public void restore() {
        if(!this.deleted) {
            throw new StateAlreadySatisfiedException("User is not soft deleted");
        }
        this.deleted = false;
        this.deletedAt = null;
        UserRestoredDomainEvent userRestoredDomainEvent = new UserRestoredDomainEvent(this.id);
        this.events.add(userRestoredDomainEvent);
    }

    public void enable() {
        if(this.enabled) {
            throw new StateAlreadySatisfiedException("User is already enabled");
        }
        this.enabled = true;
        this.auditMetadata.markUpdated();
        AccountVerifiedDomainEvent accountVerifiedDomainEvent = new AccountVerifiedDomainEvent(this.id);
        this.events.add(accountVerifiedDomainEvent);
    }

    public void lock() {
        if(this.locked) {
            throw new StateAlreadySatisfiedException("User is already locked");
        }
        this.locked = true;
        this.auditMetadata.markUpdated();
        UserLockedDomainEvent lockUserDomainEvent = new UserLockedDomainEvent(this.id);
        this.events.add(lockUserDomainEvent);
    }

    public void unlock() {
        if(!this.locked) {
            throw new StateAlreadySatisfiedException("User is already unlocked");
        }
        this.locked = false;
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