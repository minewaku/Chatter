package com.minewaku.chatter.profile.domain.model.profile.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.value.AuditMetadata;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
    @AttributeOverride(name = "value", column = @Column(name = "username", unique = true, length = 32,nullable = false))
    @NonNull
    private Username username;

    @Column(name = "avatar", length = 255)
    private String avatarHash;

    @Column(name = "banner", length = 255)
    private String bannerHash;

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

    @NonNull
    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    /*
    * PRIVATE CONSTRUCTOR
    */
    private Profile(
                @NonNull ProfileId id, 
                @NonNull Username username,     
                DisplayName displayName,
                Bio bio,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata) {

        this.id = id;
        this.username = username;
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
                @NonNull Username username, 
                DisplayName displayName,
                Bio bio,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata
            ) {

        return new Profile(id, username, displayName, bio, enablement, auditMetadata);
    }
    
    public static Profile CreateNew(
                @NonNull ProfileId id, 
                @NonNull Username username, 
                DisplayName displayName,
                Bio bio,
                @NonNull Enablement enablement
            ) {

        return new Profile(id, username, displayName, bio, enablement, new AuditMetadata());
    }

    public void isAccessible() {
        this.enablement.validateAccessible();
    }

    public boolean isUnverified() {
        return this.enablement.isUnverified();
    }

    public boolean isBanned() {
        return this.enablement.isBanned();
    }

    public boolean isSoftDeleted() {
        return this.enablement.isSoftDeleted();
    }

    

    /*
    * BEHAVIORS (MODIFY SECURE STATUSES)
    */

    public boolean softDelete(
        @NonNull Username username,
        @NonNull Enablement enablement
    ) {
        if (this.enablement.isSoftDeleted()) {
            return false; 
        }

        this.username = username;
        this.bio = null;
        this.displayName = null;
        this.avatarHash = null;
        this.bannerHash = null;
        this.enablement = enablement;
        this.auditMetadata = this.auditMetadata.markUpdated();

        //publish event
        return true;
    }


    public boolean changeAvatar(String avatarHash) {
        this.enablement.validateAccessible();
        if (Objects.equals(this.avatarHash, avatarHash)) {
            return false; 
        }
        this.avatarHash = avatarHash;

        

        return true;
    }

    public boolean changeBanner(String bannerHash) {
        this.enablement.validateAccessible();
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
        
    public boolean updateEnablement(Enablement newEnablement) {
        if (this.enablement.equals(newEnablement)) {
            return false; 
        }
        this.enablement = newEnablement;
        this.auditMetadata = this.auditMetadata.markUpdated();
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