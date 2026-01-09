package com.minewaku.chatter.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.exception.StateAlreadySatisfiedException;
import com.minewaku.chatter.domain.exception.UserNotAccessibleException;
import com.minewaku.chatter.domain.exception.UserSoftDeletedException;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Bio;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.DisplayName;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.StorageKey;
import com.minewaku.chatter.domain.value.id.UserId;

import jakarta.annotation.Nonnull;
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

    private Optional<InputAvatar> avatar;

    private Optional<InputBanner> banner;

    @NonNull
    private Username username;

    private Optional<DisplayName> displayName;

    private Optional<Bio> bio;

    @NonNull
    private final Birthday birthday;

    @Setter
    private boolean discoverable;

    @Setter
    private boolean enabled;

    @Setter
    private boolean locked;

    @Setter
    private boolean deleted;

    @NonNull
    private AuditMetadata auditMetadata;

    private Instant deletedAt;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    // Private constructor
    private User(
        @NonNull UserId id, 
        @NonNull Email email,
        InputAvatar avatar,
        InputBanner banner,
        @NonNull Username username,
        DisplayName displayName,
        Bio bio,
        @NonNull Birthday birthday, 
        @NonNull AuditMetadata auditMetadata,
        boolean discoverable,
        boolean enabled, 
        boolean locked, 
        boolean deleted, 
        Instant deletedAt) {


        StorageKey key = this.banner.map(InputBanner::getKey).orElse(null);
        this.id = id;
        this.email = email;
        this.avatar = avatar;
        this.avatarKey = avatarKey;
        this.banner = banner;
        this.bannerKey = bannerKey;
        this.username = username;
        this.birthday = birthday;
        this.displayName = displayName;
        this.bio = bio;
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
            URI avatar,
            String avatarKey,
            URI banner,
            String bannerKey,
            @NonNull Username username, 
            DisplayName displayName, 
            Bio bio,
            @NonNull Birthday birthday,
            @NonNull AuditMetadata auditMetadata,
            boolean discoverable,
            boolean enabled,
            boolean locked, 
            boolean deleted, 
            Instant deletedAt) {

        return new User(id, email, avatar, avatarKey, banner, bannerKey, username, displayName, bio, birthday, auditMetadata, discoverable, enabled, locked, deleted, deletedAt);
    }

    // Static factory for creating new data
    public static User createNew(
            @NonNull UserId id, 
            @NonNull Email email, 
            @NonNull Username username,
            @NonNull Birthday birthday,
            @NonNull AuditMetadata auditMetadata,
            @NonNull boolean locked,
            @NonNull boolean enabled,
            @NonNull boolean deleted,
            @NonNull Instant deletedAt
    ) {
                
        User user = new User(id, email, null, null, null, null, username, null, null, birthday, auditMetadata, locked, enabled, deleted, deletedAt);
        return user;
    }


    public void setDisplayName(@Nonnull DisplayName displayName) {
        if(this.displayName.equals(displayName)) {
            return;
        }

        this.displayName = displayName;
        this.auditMetadata.markUpdated();
    }

    
    public void setBio(@Nonnull Bio bio) {
        if(this.bio.equals(bio)) {
            return;
        }

        this.bio = bio;
        this.auditMetadata.markUpdated();
    }

    public void setAvatar(URI avatar, String avatarKey) {
        if((avatar == null) != (avatarKey == null)) {
            throw new IllegalArgumentException("Both avatar and avatarKey must be null or non-null");
        }
        if (avatar == null) return;

        this.avatar = avatar;
        this.avatarKey = avatarKey;
        this.auditMetadata.markUpdated();
    }   

    public void setBanner(URI banner, String bannerKey) {
        if((banner == null) != (bannerKey == null)) {
            throw new IllegalArgumentException("Both banner and bannerKey must be null or non-null");
        }
        if (banner == null) return;

        this.banner = banner;
        this.bannerKey = bannerKey;
        this.auditMetadata.markUpdated();
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

