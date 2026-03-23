package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class UserRestoredIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserRestored";

    private final String userId;

    public UserRestoredIntegrationEvent(String userId) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
    }
}
