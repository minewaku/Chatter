package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class UserEnabledIntegrationEvent extends IntegrationEvent {
    
    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserEnabled";

    private final long userId;
    
    public UserEnabledIntegrationEvent(long userId) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
    }
}
