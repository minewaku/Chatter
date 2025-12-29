package com.minewaku.chatter.domain.service.auth;

import com.minewaku.chatter.domain.exception.BusinessRuleViolationException;
import com.minewaku.chatter.domain.exception.InvalidCredentialsException;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.value.Password;

public class PasswordSecurityDomainService {
    private final PasswordHasher passwordHasher;

    public PasswordSecurityDomainService(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    public void validateCredentials(Credentials credentials, Password inputPassword) {
        if (!passwordHasher.matchesBetweenRawAndHashed(inputPassword, credentials.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    public void validatePasswordReuse(Credentials currentCredentials, Password newRawPassword) {
        if (passwordHasher.matchesBetweenRawAndHashed(newRawPassword, currentCredentials.getHashedPassword())) {
            throw new BusinessRuleViolationException("New password cannot be the same as the current password");
        }
    }
}
