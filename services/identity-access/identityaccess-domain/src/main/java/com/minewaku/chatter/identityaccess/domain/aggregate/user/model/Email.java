package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import java.util.regex.Pattern;

import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.DomainValidationException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Email {
    
    private static final String REGEX = "^[a-z0-9+_.-]+@[a-z0-9.-]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    
    private static final int MIN_LOCAL_PART_LENGTH = 1;
    private static final int MIN_DOMAIN_PART_LENGTH = 3;

    // RFC 5321/5322 standards for email
    // https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.1
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_PART_LENGTH = 255;
    private static final int MAX_TOTAL_LENGTH = 320; 

	@NonNull
    private final String value;

    public Email(@NonNull String value) {
        String processedValue = value.trim().toLowerCase();
        
        if (processedValue.length() > MAX_TOTAL_LENGTH) {
             throw new DomainValidationException("Email is too long. Max length is " + MAX_TOTAL_LENGTH);
        }

        if (processedValue.isBlank()) {
            throw new DomainValidationException("Email cannot be blank");
        }
        if (!PATTERN.matcher(processedValue).matches()) {
            throw new DomainValidationException("Invalid email format");
        }

        int atIndex = processedValue.lastIndexOf('@');
        if (atIndex > MAX_LOCAL_PART_LENGTH) {
             throw new DomainValidationException("Local part cannot exceed " + MAX_LOCAL_PART_LENGTH + " chars");
        }
        int domainLength = processedValue.length() - (atIndex + 1);
        if (domainLength > MAX_DOMAIN_PART_LENGTH) {
            throw new DomainValidationException("Domain part cannot exceed " + MAX_DOMAIN_PART_LENGTH + " chars");
        }
        
        if(atIndex < MIN_LOCAL_PART_LENGTH) {
             throw new DomainValidationException("Local part is at least " + MIN_LOCAL_PART_LENGTH + " chars");
        }   
        if (domainLength < MIN_DOMAIN_PART_LENGTH && !processedValue.equals("localhost")) {
             throw new DomainValidationException("Domain part is at least " + MIN_DOMAIN_PART_LENGTH + " chars");
        }
        
        this.value = processedValue;
    }

    public Email changeEmail(@NonNull Email newEmail) {
        if(this.equals(newEmail)) {
            throw new DomainValidationException("New email cannot be the same as the old email");
        }

        return newEmail;
    }
}
