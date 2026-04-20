package com.minewaku.chatter.identityaccess.domain.aggregate.user.event;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class UserSoftDeletedDomainEvent extends DomainEvent {

    @NonNull
    private final String userId;

    public UserSoftDeletedDomainEvent(
            @NonNull String userId
    ) {
        this.userId = userId;
    }
}
