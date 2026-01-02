package com.minewaku.chatter.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Description;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.exception.StateAlreadySatisfiedException;
import com.minewaku.chatter.domain.exception.UserNotAccessibleException;
import com.minewaku.chatter.domain.exception.UserSoftDeletedException;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.DisplayName;
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
    private URI avatar;

    @NonNull
    private URI cover;

    @NonNull
    private Username username;

    private DisplayName displayName;

    private Description description;

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

    @NonNull
    @JsonIgnore
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private User(
        @NonNull UserId id, 
        @NonNull Email email,
        @NonNull URI avatar,
        @NonNull URI cover,
        @NonNull Username username,
        DisplayName displayName,
        Description description,
        @NonNull Birthday birthday, 
        @NonNull AuditMetadata auditMetadata,
        boolean enabled, 
        boolean locked, 
        boolean deleted, 
        Instant deletedAt) {

        this.id = id;
        this.email = email;
        this.avatar = avatar;
        this.cover = cover;
        this.username = username;
        this.birthday = birthday;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
        this.locked = locked;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.auditMetadata = auditMetadata;
    }

    // Static factory for loading existing data
    public static User reconstitute(
            @NonNull UserId id, 
            @NonNull Email email,
            @NonNull URI avatar,
            @NonNull URI cover,
            @NonNull Username username, 
            DisplayName displayName, 
            Description description,
            @NonNull Birthday birthday,
            @NonNull AuditMetadata auditMetadata,
            boolean enabled,
            boolean locked, 
            boolean deleted, 
            Instant deletedAt) {

        return new User(id, email, avatar, cover, username, displayName, description, birthday, auditMetadata, enabled, locked, deleted, deletedAt);
    }

    // Static factory for creating new data
    public static User createNew(
            @NonNull UserId id, 
            @NonNull Email email, 
            @NonNull Username username,
            @NonNull Birthday birthday) {
                
        User user = new User(id, email, null, null, username, null, null, birthday, new AuditMetadata(), false, false, false, null);
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

    public void checkForDisable() {
        if (this.enabled) {
            throw new UserNotAccessibleException("User is already enabled");
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

    public void hardDeleted() {

    }

    public void softDelete() {
        if(this.deleted) {
            throw new StateAlreadySatisfiedException("User is already soft deleted");
        }
        this.deleted = true;
        this.deletedAt = Instant.now();
    }

    public void restore() {
        if(!this.deleted) {
            throw new StateAlreadySatisfiedException("User is not soft deleted");
        }
        this.deleted = false;
        this.deletedAt = null;
    }

    public void enable() {
        if(this.enabled) {
            throw new StateAlreadySatisfiedException("User is already enabled");
        }
        this.enabled = true;
        this.auditMetadata.markUpdated();
    }

    public void lock() {
        if(this.locked) {
            throw new StateAlreadySatisfiedException("User is already locked");
        }
        this.locked = true;
        this.auditMetadata.markUpdated();
    }

    public void unlock() {
        if(!this.locked) {
            throw new StateAlreadySatisfiedException("User is already unlocked");
        }
        this.locked = false;
        this.auditMetadata.markUpdated();
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

