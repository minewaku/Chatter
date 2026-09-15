package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class PhoneNumber {

    private final String value;

    public PhoneNumber(@NonNull String value) {
        if (value.isBlank()) {
            throw new DomainValidationException("Phone number cannot be blank");
        }
        this.value = value;
    }
}
