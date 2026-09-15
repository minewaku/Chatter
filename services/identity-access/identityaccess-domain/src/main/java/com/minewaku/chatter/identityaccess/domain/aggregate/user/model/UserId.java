package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class UserId implements Id {

    private final Long value;

    public UserId(@NonNull Long value) {
        if (value <= 0) {
            throw new DomainValidationException("UserId value cannot be smaller than 1");
        }

        this.value = value;
    }
}
