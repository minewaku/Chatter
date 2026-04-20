package com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class Credentials {

    private static final long PASSWORD_CHANGE_MIN_INTERVAL_MINUTES = 5L;

    @NonNull
    private HashedPassword hashedPassword;

    private Instant modifiedAt;

    // Private constructor
    private Credentials(
            @NonNull HashedPassword hashedPassword,
            Instant modifiedAt) {  

        this.hashedPassword = hashedPassword;
        this.modifiedAt = modifiedAt;
    }

    public static Credentials createNew(@NonNull HashedPassword hashedPassword) {
        return new Credentials(hashedPassword, null);
    }

    public static Credentials reconstitute(
            @NonNull HashedPassword hashedPassword,
            Instant modifiedAt) {

        return new Credentials(hashedPassword, modifiedAt);
    }


    public Credentials anonymize() {
        HashedPassword anonymizedPassword = new HashedPassword("anonymized", "anonymized", new byte[16]);
        
        Credentials anonymizedCredentials = new Credentials(anonymizedPassword, Instant.now());
        return anonymizedCredentials;
    }

    public Credentials changePassword (
            @NonNull PasswordHasher passwordHasher,
            @NonNull Password oldPassword, 
            @NonNull Password newPassword) {

        if (!passwordHasher.matchesRawAndHashedPassword(oldPassword, this.hashedPassword)) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        if (!passwordHasher.matchesRawAndHashedPassword(newPassword, this.hashedPassword)) {
            throw new BusinessRuleViolationException("New password cannot be the same as the old password");
        }

        if (this.modifiedAt != null) {
            Instant nextAllowedTime = this.modifiedAt.plus(PASSWORD_CHANGE_MIN_INTERVAL_MINUTES, ChronoUnit.MINUTES);
            Instant now = Instant.now();
            if (now.isBefore(nextAllowedTime)) {
                throw new BusinessRuleViolationException(
                    String.format("you only allow to change password once every %d minutes", PASSWORD_CHANGE_MIN_INTERVAL_MINUTES)
                );
            }
        }

        HashedPassword newHashedPassword = passwordHasher.hash(newPassword);

        return new Credentials(newHashedPassword, Instant.now());
    }

    public void validateHashedPassword(@NonNull PasswordHasher passwordHasher, Password rawPassword) {
         if (!passwordHasher.matchesRawAndHashedPassword(rawPassword, this.hashedPassword)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
}
