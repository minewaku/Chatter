package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ConfirmationTokenCreatedDomainEvent extends DomainEvent {

    @NonNull
    private final String token;

    @NonNull
    private final String userId;

    @NonNull
    private final String email;

    @NonNull
    private final String duration;

    @NonNull
    private final String expiresAt;

    public ConfirmationTokenCreatedDomainEvent(
            @NonNull String token,
            @NonNull String userId,
            @NonNull String email,
            @NonNull String duration,
            @NonNull String expiresAt
    ) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.duration = duration;
        this.expiresAt = expiresAt;
    }
}