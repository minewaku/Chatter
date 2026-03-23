package com.minewaku.chatter.identityaccess.domain.aggregate.session.event;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SessionCreatedDomainEvent extends DomainEvent {

    @NonNull
    private final String sessionId;

    @NonNull
    private final String userId;

    @NonNull
    private final String timestamp;

    public SessionCreatedDomainEvent(
            @NonNull String sessionId,
            @NonNull String userId,
            @NonNull String timestamp
    ) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.timestamp = timestamp;
    }
}
