package com.minewaku.chatter.identityaccess.domain.aggregate.session.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.Id;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class SessionId implements Id {

    private final String value;

    public SessionId(@NonNull String value) {
        if (value.isBlank()) {
            throw new DomainValidationException("SessionId value cannot be blank");
        }

        this.value = value;
    }
}
