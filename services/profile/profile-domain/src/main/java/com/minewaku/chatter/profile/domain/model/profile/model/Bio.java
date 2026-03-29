package com.minewaku.chatter.profile.domain.model.profile.model;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Embeddable
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bio {

    private static final int MAX_LENGTH = 190;
    
    @NonNull
    private String value;

    public Bio(@NonNull String value) {
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                "Bio is too long. Max length is " + MAX_LENGTH + " characters."
            );
        }

        this.value = value;
    }
}
