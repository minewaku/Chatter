package com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ConfirmationTokenId implements Id {

    private final String value;

    public ConfirmationTokenId(@NonNull String value) {
        this.value = value;
    }
}
