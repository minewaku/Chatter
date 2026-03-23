package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ConfirmationTokenVerifiedDomainEvent extends DomainEvent{

    @NonNull
    private final String userId;

    @NonNull
    private final String token;


    public ConfirmationTokenVerifiedDomainEvent(
            @NonNull String userId,
            @NonNull String token
    ) {
        this.userId = userId;
        this.token = token;
    }
}
