package com.minewaku.chatter.profile.domain.model.profile.event;

import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;

@Getter
public class BannerReplacedDomainEvent extends DomainEvent{

    private final String namespace = Namespace.USER_BANNERS.name();
    private final String oldHashFile;
    private final String newHashFile;

    public BannerReplacedDomainEvent(
            String oldHashFile,
            String newHashFile) {

        this.oldHashFile = oldHashFile;
        this.newHashFile = newHashFile;
    }
}
