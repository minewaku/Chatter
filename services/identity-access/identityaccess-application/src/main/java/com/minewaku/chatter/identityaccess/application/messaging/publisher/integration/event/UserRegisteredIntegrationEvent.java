package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import java.time.Instant;
import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class UserRegisteredIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserRegistered";

    private Long userId;
    private String email;
    private String username;
    private LocalDate birthday;
    private boolean enabled;
    private boolean deleted;
    private boolean locked;
    private Instant deletedAt;

    public UserRegisteredIntegrationEvent() {
        super(AGGREGATE_TYPE, EVENT_TYPE); 
    }
    
    public UserRegisteredIntegrationEvent(
            Long userId, 
            String email, 
            String username, 
            LocalDate birthday, 
            boolean enabled, 
            boolean deleted, 
            boolean locked, 
            Instant deletedAt) {

        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.enabled = enabled;
        this.deleted = deleted;
        this.locked = locked;
        this.deletedAt = deletedAt;
    }
}
