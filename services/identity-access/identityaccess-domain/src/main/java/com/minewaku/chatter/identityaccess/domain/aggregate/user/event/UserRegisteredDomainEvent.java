package com.minewaku.chatter.identityaccess.domain.aggregate.user.event;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class UserRegisteredDomainEvent extends DomainEvent {

    @NonNull
    private final String userId;

    @NonNull
    private final String email;

    public UserRegisteredDomainEvent(
            @NonNull String userId,
            @NonNull String email
    ) {
        this.userId = userId;
        this.email = email;
    }
}
