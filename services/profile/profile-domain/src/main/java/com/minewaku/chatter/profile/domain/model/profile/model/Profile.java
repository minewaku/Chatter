package com.minewaku.chatter.profile.domain.model.profile.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import com.minewaku.chatter.profile.domain.model.profile.event.AvatarReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.model.profile.event.BannerReplacedDomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.profile.domain.sharedkernel.value.AuditMetadata;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Table("profile")
@ToString
public class Profile {

    @Id
    private ProfileId id;

    @Embedded.Nullable
    private Username username;

    @Column("avatar")
    private String avatarHash;

    @Column("banner")
    private String bannerHash;

    @Embedded.Nullable
    private DisplayName displayName;

    @Embedded.Nullable
    private Bio bio;

    @Embedded.Nullable
    private Enablement enablement;

    @Embedded.Nullable
    private AuditMetadata auditMetadata;

    @Version
    private Integer version;

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
    @PersistenceCreator
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

        return new Profile(id, username, displayName, bio, enablement, AuditMetadata.createNew());
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
        AvatarReplacedDomainEvent avatarEvent = new AvatarReplacedDomainEvent(avatarHash);
        BannerReplacedDomainEvent bannerEvent = new BannerReplacedDomainEvent(bannerHash);

        this.username = username;
        this.bio = null;
        this.displayName = null;
        this.avatarHash = null;
        this.bannerHash = null;
        this.enablement = enablement;
        this.auditMetadata = this.auditMetadata.markUpdated();

        domainEvents.add(avatarEvent);
        domainEvents.add(bannerEvent);
        return true;
    }


    public boolean changeAvatar(String avatarHash) {
        this.enablement.validateAccessible();
        if (Objects.equals(this.avatarHash, avatarHash)) {
            return false; 
        }

        if (this.avatarHash != null) {
            this.domainEvents.add(new AvatarReplacedDomainEvent(
                this.avatarHash
            ));
        }

        this.avatarHash = avatarHash;
        return true;
    }

    public boolean changeBanner(String bannerHash) {
        this.enablement.validateAccessible();
        if (Objects.equals(this.bannerHash, bannerHash)) {
            return false;
        }

        if (this.bannerHash != null) {
            this.domainEvents.add(new BannerReplacedDomainEvent(
                this.bannerHash
            ));
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