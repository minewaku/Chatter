package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Credentials;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Password;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.AggregateRoot;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.AuditMetadata;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class User extends AggregateRoot<UserId> {

    private final UserId id;

    private Email email;

    private Username username;

    private final Birthday birthday;

    private Enablement enablement;

    private AuditMetadata auditMetadata;

    private Credentials credentials;

    private final Integer version;

    private final List<DomainEvent> events = new ArrayList<>();

    private User(
            @NonNull UserId id,
            @NonNull Email email,
            @NonNull Username username,
            @NonNull Birthday birthday,
            @NonNull Enablement enablement,
            @NonNull AuditMetadata auditMetadata,
            @NonNull Credentials credentials,
            Integer version) {

        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.enablement = enablement;
        this.auditMetadata = auditMetadata;
        this.credentials = credentials;
        this.version = version;
    }

    public static User createNew(
            @NonNull UserId id,
            @NonNull Email email,
            @NonNull Username username,
            @NonNull Birthday birthday,
            @NonNull HashedPassword hashedPassword) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(email, "email cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(birthday, "birthday cannot be null");
        Objects.requireNonNull(hashedPassword, "hashedPassword cannot be null");

        Enablement enablement = new Enablement();
        AuditMetadata auditMetadata = new AuditMetadata();
        Credentials credentials = Credentials.createNew(hashedPassword);
        User user = new User(id, email, username, birthday, enablement, auditMetadata, credentials, null);

        UserRegisteredDomainEvent userRegisteredDomainEvent = new UserRegisteredDomainEvent(
                user.getId().getValue().toString(),
                user.getEmail().getValue());

        user.getEvents().add(userRegisteredDomainEvent);
        return user;
    }

    public static User register(
            @NonNull UserId id,
            @NonNull Email email,
            @NonNull Username username,
            @NonNull Birthday birthday,
            @NonNull HashedPassword hashedPassword) {
        return createNew(id, email, username, birthday, hashedPassword);
    }

    public static User reconstitute(
            @NonNull UserId id,
            @NonNull Email email,
            @NonNull Username username,
            @NonNull Birthday birthday,
            @NonNull Enablement enablement,
            @NonNull AuditMetadata auditMetadata,
            @NonNull Credentials credentials,
            Integer version) {
        return new User(id, email, username, birthday, enablement, auditMetadata, credentials, version);
    }

    @Override
    public UserId getId() {
        return this.id;
    }

    public void isAccessible() {
        this.enablement.validateAccessible();
    }

    public boolean isUnverified() {
        return this.enablement.isUnverified();
    }

    public boolean isBanned() {
        return this.enablement.isBanned();
    }

    public boolean isSoftDeleted() {
        return this.enablement.isSoftDeleted();
    }

    public boolean softDelete() {
        Enablement newEnablement = this.enablement.softDelete();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }

        this.enablement = newEnablement;
        this.credentials = this.credentials.anonymize();
        String anonymizedSuffix = this.id.getValue().toString();
        this.email = new Email("deleted_" + anonymizedSuffix + "@anonymized.local");
        this.username = new Username("deleted_user_" + anonymizedSuffix);
        this.auditMetadata = this.auditMetadata.markUpdated();

        UserSoftDeletedDomainEvent userSoftDeletedDomainEvent = new UserSoftDeletedDomainEvent(this.id.getValue().toString());
        this.events.add(userSoftDeletedDomainEvent);

        return true;
    }

    public boolean enable() {
        Enablement newEnablement = this.enablement.enable();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }

        this.enablement = newEnablement;
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public boolean disable() {
        Enablement newEnablement = this.enablement.disable();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }

        this.enablement = newEnablement;
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public boolean lock() {
        Enablement newEnablement = this.enablement.lock();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }

        this.enablement = newEnablement;
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public boolean unlock() {
        Enablement newEnablement = this.enablement.unlock();
        if (this.enablement.equals(newEnablement)) {
            return false;
        }

        this.enablement = newEnablement;
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public void authenticate(@NonNull PasswordHasher passwordHasher, @NonNull Password password) {
        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(passwordHasher, password);
    }

    public boolean changeUsername(
            @NonNull PasswordHasher passwordHasher,
            @NonNull Username newUsername,
            @NonNull Password password) {

        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(passwordHasher, password);

        if (this.username.equals(newUsername)) {
            return false;
        }

        this.username = this.username.changeUsername(newUsername);
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public boolean changeEmail(
            @NonNull PasswordHasher passwordHasher,
            @NonNull Email newEmail,
            @NonNull Password password) {

        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(passwordHasher, password);

        if (this.email.equals(newEmail)) {
            return false;
        }

        this.email = this.email.changeEmail(newEmail);
        this.enablement = this.enablement.disable();
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }

    public boolean changePassword(@NonNull PasswordHasher passwordHasher, @NonNull Password oldPassword, @NonNull Password newPassword) {
        this.enablement.validateAccessible();

        Credentials newCredentials = this.credentials.changePassword(passwordHasher, oldPassword, newPassword);
        if (this.credentials.equals(newCredentials)) {
            return false;
        }

        this.credentials = newCredentials;
        this.auditMetadata = this.auditMetadata.markUpdated();
        return true;
    }
}