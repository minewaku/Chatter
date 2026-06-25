package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ProfileUpdatedIntegrationEvent extends IntegrationEvent {

    public static final String AGGREGATE_TYPE = "None";
    public static final String EVENT_TYPE = "DeleteFileStorage";

    private final Long profileId;
    private final String displayName;
    private final String bio;

    public ProfileUpdatedIntegrationEvent(
            @NonNull Long profileId, 
            String displayName, 
            String bio) {

        super(AGGREGATE_TYPE, EVENT_TYPE);

        this.profileId = profileId;
        this.displayName = displayName;
        this.bio = bio;
    }
}
