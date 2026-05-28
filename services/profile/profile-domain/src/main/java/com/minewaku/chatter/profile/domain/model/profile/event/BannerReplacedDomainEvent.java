package com.minewaku.chatter.profile.domain.model.profile.event;

import com.minewaku.chatter.profile.domain.model.file.model.Namespace;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class BannerReplacedDomainEvent extends DomainEvent{

    private final String namespace = Namespace.USER_BANNERS.name();
    private final String hashFile;

    public BannerReplacedDomainEvent(
            @NonNull String hashFile) {

        this.hashFile = hashFile;
    }
}
