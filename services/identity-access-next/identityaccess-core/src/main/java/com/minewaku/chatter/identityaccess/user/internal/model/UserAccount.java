package com.minewaku.chatter.identityaccess.user.internal.model;

import com.minewaku.chatter.identityaccess.user.internal.exception.InvalidAccountStateTransitionException;
import com.minewaku.chatter.identityaccess.user.internal.exception.InvalidCredentialsException;
import com.minewaku.chatter.identityaccess.user.internal.port.PasswordHasher;
import java.time.Instant;
import java.util.Objects;

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

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public Username username() {
        return username;
    }

    public Birthday birthday() {
        return birthday;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Long version() {
        return version;
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
            PlainPassword currentPassword, PlainPassword newPassword, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(currentPassword, passwordHasher, "change password");
        if (passwordHasher.matches(newPassword, passwordHash)) {
            return false;
        }
        passwordHash = passwordHasher.hash(newPassword);
        markUpdated();
        return true;
    }

    public boolean changeUsername(PlainPassword currentPassword, Username newUsername, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(currentPassword, passwordHasher, "change username");
        if (username.equals(newUsername)) {
            return false;
        }
        username = newUsername;
        markUpdated();
        return true;
    }

    public boolean changeEmail(PlainPassword currentPassword, Email newEmail, PasswordHasher passwordHasher) {
        verifyAccessibleCredentials(currentPassword, passwordHasher, "change email");
        if (email.equals(newEmail)) {
            return false;
        }
        email = newEmail;
        markUpdated();
        return true;
    }

    private void verifyAccessibleCredentials(
            PlainPassword currentPassword, PasswordHasher passwordHasher, String operation) {
        if (!isAccessible()) {
            throw new InvalidAccountStateTransitionException(status, operation);
        }
        if (!passwordHasher.matches(currentPassword, passwordHash)) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
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
