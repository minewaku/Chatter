package com.minewaku.chatter.identityaccess.user.internal.model;

import com.minewaku.chatter.identityaccess.user.api.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.api.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

@Getter
public class UserAccount {

    private final UserId id;
    private Email email;

    private Username username;
    private final Birthday birthday;
    private AccountStatus status;
    private Instant deletedAt;
    private PasswordHash passwordHash;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private UserAccount(
            UserId id,
            Email email,
            Username username,
            Birthday birthday,
            AccountStatus status,
            Instant deletedAt,
            PasswordHash passwordHash,
            Instant createdAt,
            Instant updatedAt,
            Long version) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.username = Objects.requireNonNull(username, "username is required");
        this.birthday = Objects.requireNonNull(birthday, "birthday is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.deletedAt = deletedAt;
        this.passwordHash = Objects.requireNonNull(passwordHash, "password hash is required");
        this.createdAt = Objects.requireNonNull(createdAt, "created time is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updated time is required");
        this.version = version;
    }

    public static UserAccount register(
            UserId id, Email email, Username username, Birthday birthday, PasswordHash hash) {
        Instant now = Instant.now();
        return new UserAccount(
                id, email, username, birthday, AccountStatus.PENDING_VERIFICATION, null, hash, now, now, null);
    }

    public static UserAccount reconstitute(
            UserId id,
            Email email,
            Username username,
            Birthday birthday,
            AccountStatus status,
            Instant deletedAt,
            PasswordHash passwordHash,
            Instant createdAt,
            Instant updatedAt,
            Long version) {
        return new UserAccount(
                id, email, username, birthday, status, deletedAt, passwordHash, createdAt, updatedAt, version);
    }

    public boolean isAccessible() {
        return status.isAccessible();
    }

    private void markUpdated() {
        updatedAt = Instant.now();
    }

    private boolean transition(AccountStatus expected, AccountStatus target, String operation) {
        if (status == target) {
            return false;
        }
        if (status != expected) {
            throw new InvalidAccountStateTransitionException(status, operation);
        }
        status = target;
        markUpdated();
        return true;
    }

    public boolean activateAfterVerification() {
        return transition(AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE, "activate after verification");
    }

    public boolean suspend() {
        return transition(AccountStatus.ACTIVE, AccountStatus.SUSPENDED, "suspend");
    }

    public boolean reinstate() {
        return transition(AccountStatus.SUSPENDED, AccountStatus.ACTIVE, "reinstate");
    }

    public boolean lock() {
        return transition(AccountStatus.ACTIVE, AccountStatus.LOCKED, "lock");
    }

    public boolean unlock() {
        return transition(AccountStatus.LOCKED, AccountStatus.ACTIVE, "unlock");
    }

    public boolean changePassword(
            String currentPassword, PlainPassword newPassword, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(currentPassword, passwordHasher, "change password");
        if (passwordHasher.matches(newPassword, passwordHash)) {
            return false;
        }
        passwordHash = passwordHasher.hash(newPassword);
        markUpdated();
        return true;
    }

    public boolean changeUsername(Username newUsername) {
        requireAccessible("change username");
        if (username.equals(newUsername)) {
            return false;
        }
        username = newUsername;
        markUpdated();
        return true;
    }

    /**
     * Checks that this account may ask to move to {@code newEmail}. Returns false when it is already the current
     * email. Nothing is changed: the email only moves once the new address is verified.
     */
    public boolean authorizeEmailChange(String currentPassword, Email newEmail, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(currentPassword, passwordHasher, "change email");
        return !email.equals(newEmail);
    }

    public void applyVerifiedEmail(Email newEmail) {
        requireAccessible("confirm email change");
        email = newEmail;
        markUpdated();
    }

    /**
     * Confirms the person acting for this account by their current password, for actions that change nothing here
     * (such as ending sessions). Follows the one rule shared by every action that asks for the password: the status is
     * checked first, so a blocked account is reported as such whatever password was sent, then the password.
     */
    public void confirmCredentials(String presentedPassword, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(presentedPassword, passwordHasher, "confirm current password");
    }

    /** Refuses {@code operation} unless this account is accessible. */
    public void requireAccessible(String operation) {
        if (!isAccessible()) {
            throw new InvalidAccountStateTransitionException(status, operation);
        }
    }

    private void verifyAccessibleCredentials(
            String presentedPassword, PasswordHasher passwordHasher, String operation) {
        requireAccessible(operation);
        verifyPassword(presentedPassword, passwordHasher);
    }

    private void verifyPassword(String presentedPassword, PasswordHasher passwordHasher) {
        if (!passwordHasher.matches(PlainPassword.presented(presentedPassword), passwordHash)) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
    }

    /** Closes the account on the owner's request. Unlike the other credential-checked operations, any status may close. */
    public boolean close(String currentPassword, PasswordHasher passwordHasher) {
        verifyPassword(currentPassword, passwordHasher);
        return softDelete();
    }

    public boolean softDelete() {
        if (status == AccountStatus.DELETED) {
            return false;
        }
        Instant now = Instant.now();
        status = AccountStatus.DELETED;
        deletedAt = now;
        email = new Email("deleted-" + id.value() + "@deleted.invalid");
        username = new Username("deleted_" + id.value());
        passwordHash = new PasswordHash("deleted", "unusable", new byte[] {0});
        updatedAt = now;
        return true;
    }
}
