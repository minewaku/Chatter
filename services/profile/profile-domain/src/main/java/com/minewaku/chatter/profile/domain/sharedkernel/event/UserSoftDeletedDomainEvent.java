package com.minewaku.chatter.profile.domain.sharedkernel.event;

import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class UserSoftDeletedDomainEvent extends DomainEvent {
    
    @NonNull
    private final ProfileId profileId;

    @NonNull
    private final String avatarHash;

    @NonNull
    private final String bannerHash;

    public UserSoftDeletedDomainEvent(
            @NonNull ProfileId profileId, 
            @NonNull String avatarHash, 
            @NonNull String bannerHash) {
                
        this.profileId = profileId;
        this.avatarHash = avatarHash;
        this.bannerHash = bannerHash;
    }
}
