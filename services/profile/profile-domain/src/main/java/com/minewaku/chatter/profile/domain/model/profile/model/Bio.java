package com.minewaku.chatter.profile.domain.model.profile.model;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import com.minewaku.chatter.profile.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Bio {

    private static final int MAX_LENGTH = 190;
    
    @Column("bio")
    private String value;

    @PersistenceCreator
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
