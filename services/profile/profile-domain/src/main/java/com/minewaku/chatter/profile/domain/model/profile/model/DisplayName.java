package com.minewaku.chatter.profile.domain.model.profile.model;

import java.util.regex.Pattern;

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
public final class DisplayName {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 32;
    private static final String REGEX = "^[^\\n\\r\\t]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    @NonNull
    private String value;

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