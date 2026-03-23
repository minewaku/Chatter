package com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.exception.BusinessRuleViolationException;

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



    public Credentials changePassword (
            @NonNull HashedPassword oldHashedPassword, 
            @NonNull HashedPassword newHashedPassword) {

        if (oldHashedPassword.equals(newHashedPassword)) {
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

        return new Credentials(newHashedPassword, Instant.now());
    }

    public void validateHashedPassword(HashedPassword inputHashedPassword) {
        if (!this.hashedPassword.equals(inputHashedPassword)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
}
