package com.minewaku.chatter.profile.domain.model.profile.model;

import java.util.regex.Pattern;

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
public final class DisplayName {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;
    private static final String REGEX = "^[^\\n\\r\\t]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    @Column("display_name")
    private String value;

    
    @PersistenceCreator
    public DisplayName(@NonNull String value) {
        String processedValue = value.trim();

        if (processedValue.isBlank()) {
            throw new DomainValidationException("Display name cannot be empty or blank");
        }
        if (processedValue.length() < MIN_LENGTH || processedValue.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                "Display name must be between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!PATTERN.matcher(processedValue).matches()) {
            throw new DomainValidationException("Display name cannot contain newlines, tabs, or control characters");
        }

        this.value = processedValue;
    }
}