package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Username {

    private static final String USERNAME_PATTERN = "^(?!.*\\.\\.)[A-Za-z0-9_](?:[A-Za-z0-9_.]{0,30}[A-Za-z0-9_])?$";

    @NonNull
    private final String value;

    public Username(@NonNull String value) {

        if (value.isBlank()) {
            throw new DomainValidationException("Username cannot be blank");
        }
        if (!value.matches(USERNAME_PATTERN)) {
            throw new DomainValidationException(
                    "Invalid username. Must be 2-32 chars, letters, digits, '_' or '.', cannot start/end with '.' or contain '..': "
                            + value);
        }
        this.value = value;
    }

    public Username changeUsername(@NonNull Username newUsername) {
        if(this.equals(newUsername)) {
            throw new DomainValidationException("New username cannot be the same as the old username");
        }

        return newUsername;
    }
}
