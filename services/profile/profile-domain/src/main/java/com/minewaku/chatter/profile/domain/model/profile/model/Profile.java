package com.minewaku.chatter.profile.domain.model.profile.model;

import java.util.Objects;

import com.minewaku.chatter.profile.domain.sharedkernel.value.AuditMetadata;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Table(name = "\"profile\"")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @EmbeddedId
    @AttributeOverride(name = "value", column = @Column(name = "id"))
    @NonNull
    private ProfileId id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", unique = true, nullable = false))
    @NonNull
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "username", unique = true, length = 32,nullable = false))
    @NonNull
    private Username username;

    @Column(name = "avatar", length = 255)
    private String avatarHash;

    @Column(name = "banner", length = 255)
    private String bannerHash;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "birthday", nullable = false))
    @NonNull
    private Birthday birthday;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "display_name", length = 32))
    private DisplayName displayName;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "bio", length = 190))
    private Bio bio;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "enabled", column = @Column(name = "is_enabled")),
        @AttributeOverride(name = "locked", column = @Column(name = "is_locked")),
        @AttributeOverride(name = "deletionStatus.deleted", column = @Column(name = "is_deleted")),
        @AttributeOverride(name = "deletionStatus.deletedAt", column = @Column(name = "deleted_at"))
    })
    @NonNull
    private Enablement enablement;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "modifiedAt", column = @Column(name = "modified_at"))
    })
    @NonNull
    private AuditMetadata auditMetadata;

    /*
    * PRIVATE CONSTRUCTOR
    */
    private Profile(
                @NonNull ProfileId id, 
                @NonNull Email email,
                @NonNull Username username,     
                @NonNull Birthday birthday,
                DisplayName displayName,
                Bio bio,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata) {

        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.displayName = displayName;
        this.bio = bio;
        this.enablement = enablement;
        this.auditMetadata = auditMetadata;
    }

    /*
    * STATIC FACTORIES
    */
    public static Profile reconstitute(
                @NonNull ProfileId id, 
                @NonNull Email email,
                @NonNull Username username, 
                @NonNull Birthday birthday,
                DisplayName displayName,
                Bio bio,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata
            ) {

        return new Profile(id, email, username, birthday, displayName, bio, enablement, auditMetadata);
    }

    /*
    * BEHAVIORS
    */
    public boolean softDelete() {
        var newEnablement = this.enablement.softDelete();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean restore() {
        var newEnablement = this.enablement.restore();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean enable() {
        var newEnablement = this.enablement.enable();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean disable() {
        var newEnablement = this.enablement.disable();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean lock() {
        var newEnablement = this.enablement.lock();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean unlock() {
        var newEnablement = this.enablement.unlock();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }
        this.enablement = newEnablement;
        return true;
    }

    public boolean changeAvatar(String avatarHash) {
        if (Objects.equals(this.avatarHash, avatarHash)) {
            return false; 
        }
        this.avatarHash = avatarHash;
        return true;
    }

    public boolean changeBanner(String bannerHash) {
        if (Objects.equals(this.bannerHash, bannerHash)) {
            return false;
        }
        this.bannerHash = bannerHash;
        return true;
    }

    public boolean changeDisplayName(DisplayName newDisplayName) {
        this.enablement.validateAccessible();
        if (this.displayName != null && this.displayName.equals(newDisplayName)) {
            return false; 
        }
        this.displayName = newDisplayName;
        return true;
    }

    public boolean changeBio(Bio newBio) {
        this.enablement.validateAccessible();
        if (this.bio != null && this.bio.equals(newBio)) {
            return false;
        }
        this.bio = newBio;
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Profile))
            return false;
        Profile other = (Profile) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}