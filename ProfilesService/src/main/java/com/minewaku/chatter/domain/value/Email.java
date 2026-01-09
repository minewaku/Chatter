package com.minewaku.chatter.domain.value;

import java.util.regex.Pattern;

import com.minewaku.chatter.domain.exception.DomainValidationException;

import lombok.NonNull;

public record Email(@NonNull String value) {
    
    private static final String REGEX = "^[a-z0-9+_.-]+@[a-z0-9.-]+$";
    private static final Pattern PATTERN = Pattern.compile(REGEX);
    
	private static final int MIN_LOCAL_PART_LENGTH = 1;
	private static final int MIN_DOMAIN_PART_LENGTH = 3;

    // RFC 5321/5322 standards for email
	// https://datatracker.ietf.org/doc/html/rfc5321#section-4.5.3.1.1
    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_PART_LENGTH = 255;
    private static final int MAX_TOTAL_LENGTH = 320; 

    public Email {
        value = value.trim().toLowerCase();
        if (value.length() > MAX_TOTAL_LENGTH) {
             throw new DomainValidationException("Email is too long. Max length is " + MAX_TOTAL_LENGTH);
        }

        if (value.isBlank()) {
            throw new DomainValidationException("Email cannot be blank");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainValidationException("Invalid email format");
        }

        int atIndex = value.lastIndexOf('@');
        if (atIndex > MAX_LOCAL_PART_LENGTH) {
             throw new DomainValidationException("Local part cannot exceed " + MAX_LOCAL_PART_LENGTH + " chars");
        }
        int domainLength = value.length() - (atIndex + 1);
        if (domainLength > MAX_DOMAIN_PART_LENGTH) {
            throw new DomainValidationException("Domain part cannot exceed " + MAX_DOMAIN_PART_LENGTH + " chars");
        }
        
		if(atIndex < MIN_LOCAL_PART_LENGTH) {
			 throw new DomainValidationException("Local part is at least " + MIN_LOCAL_PART_LENGTH + " chars");
		}	
        if (domainLength < MIN_DOMAIN_PART_LENGTH && !value.equals("localhost")) {
             throw new DomainValidationException("Domain part is at least " + MIN_DOMAIN_PART_LENGTH + " chars");
        }
    }

    public static Email of(String value) {
        return value == null ? null : new Email(value);
    }
}
