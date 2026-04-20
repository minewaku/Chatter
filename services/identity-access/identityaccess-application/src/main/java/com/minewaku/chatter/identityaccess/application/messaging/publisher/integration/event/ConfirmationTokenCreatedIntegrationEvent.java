package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class ConfirmationTokenCreatedIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "ConfirmationToken";
    private static final String EVENT_TYPE = "ConfirmationTokenCreated";

    private String token;
    private Long userId;
    private String email;
    private String duration;
    private String expiresAt;

    public ConfirmationTokenCreatedIntegrationEvent() {
        super(AGGREGATE_TYPE, EVENT_TYPE); 
    }

    public ConfirmationTokenCreatedIntegrationEvent(
            String token, 
            Long userId, 
            String email, 
            String duration, 
            String expiresAt) {
        
        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.duration = duration;
        this.expiresAt = expiresAt;
    }
    
}

