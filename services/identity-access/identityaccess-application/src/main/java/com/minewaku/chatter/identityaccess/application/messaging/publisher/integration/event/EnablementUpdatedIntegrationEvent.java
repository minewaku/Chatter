package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import java.time.Instant;

import io.github.resilience4j.core.lang.NonNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class EnablementUpdatedIntegrationEvent extends IntegrationEvent {

    private static final String AGGREGATE_TYPE = "User";
    private static final String EVENT_TYPE = "EnablementUpdated";

    private Long userId;
    private boolean enabled;
    private boolean locked;
    private boolean deleted;
    private Instant deletedAt;

    public EnablementUpdatedIntegrationEvent() {
        super(AGGREGATE_TYPE, EVENT_TYPE); 
    }
    
    public EnablementUpdatedIntegrationEvent(
            @NonNull Long userId,
            boolean enabled,
            boolean locked,
            boolean deleted,
            Instant deletedAt) {

        super(AGGREGATE_TYPE, EVENT_TYPE);
        this.userId = userId;
        this.enabled = enabled;
        this.locked = locked;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }    
} 
