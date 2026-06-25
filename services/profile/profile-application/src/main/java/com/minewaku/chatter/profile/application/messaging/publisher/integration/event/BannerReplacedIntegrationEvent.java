package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import com.minewaku.chatter.profile.domain.model.asset.model.Namespace;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class BannerReplacedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "Profile";
    public static final String EVENT_TYPE = "BannerReplaced";

    private final Long profileId;
    private final String namespace = Namespace.USER_BANNERS.name();
    private final String oldHashFile;
    private final String newHashFile;

    public BannerReplacedIntegrationEvent(
            @NonNull Long profileId,
            String oldHashFile,
            String newHashFile) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.profileId = profileId;
        this.oldHashFile = oldHashFile;
        this.newHashFile = newHashFile;
    }
}