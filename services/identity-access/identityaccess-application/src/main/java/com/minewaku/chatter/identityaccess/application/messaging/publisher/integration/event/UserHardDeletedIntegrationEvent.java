package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class UserHardDeletedIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserHardDeleted";

    private final String userId;
    
    public UserHardDeletedIntegrationEvent(String userId) {
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
    }    
}
