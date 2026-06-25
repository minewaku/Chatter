package com.minewaku.chatter.profile.domain.model.profile.event;

import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

import io.micrometer.common.lang.NonNull;
import lombok.Getter;

@Getter
public class BannerReplacedDomainEvent extends DomainEvent{

    private final Long profileId;   
    private final String namespace = Namespace.USER_BANNERS.name();
    private final String oldHashFile;
    private final String newHashFile;

    public BannerReplacedDomainEvent(
            @NonNull Long profileId,
            String oldHashFile,
            String newHashFile) {

        this.profileId = profileId;
        this.oldHashFile = oldHashFile;
        this.newHashFile = newHashFile;
    }
}
