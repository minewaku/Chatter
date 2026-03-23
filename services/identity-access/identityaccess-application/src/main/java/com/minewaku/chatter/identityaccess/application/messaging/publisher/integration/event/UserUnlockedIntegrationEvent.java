package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class UserUnlockedIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserUnlocked";

    private final String userId;
    
    public UserUnlockedIntegrationEvent(String userId) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
    }
}