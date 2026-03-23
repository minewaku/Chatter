package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import java.time.Instant;
import java.time.LocalDate;

import lombok.Getter;

@Getter
public class UserRegisteredIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "UserRegisted";

    private final long userId;
    private final String email;
    private final String username;
    private final LocalDate birthday;
    private final boolean enabled;
    private final boolean deleted;
    private final boolean locked;
    private Instant deletedAt;

    
    public UserRegisteredIntegrationEvent(long userId, String email, String username, 
                                        LocalDate birthday, boolean enabled, 
                                        boolean deleted, boolean locked, Instant deletedAt) {
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
