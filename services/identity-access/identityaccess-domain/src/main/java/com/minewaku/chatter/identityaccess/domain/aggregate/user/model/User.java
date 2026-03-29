package com.minewaku.chatter.identityaccess.domain.aggregate.user.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.Credentials;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.AuditMetadata;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class User {

    @NonNull
    private final UserId id;

    @NonNull
    private Email email;

    @NonNull
    private Username username;

    @NonNull
    private final Birthday birthday;

    //implement phone number value object and all invariants belongs to it

    private Instant lastLoginAt;

    @NonNull
    private Enablement enablement;

    @NonNull
    private AuditMetadata auditMetadata;
    

    @NonNull
    private Credentials credentials;

    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();


    
    /*
    * PRIVATE CONSTRUCTOR
    */
    private User(
                @NonNull UserId id, 
                @NonNull Email email,
                @NonNull Username username,     
                @NonNull Birthday birthday,
                Instant lastLoginAt,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata,
                @NonNull Credentials credentials) {

        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.lastLoginAt = lastLoginAt;
        this.enablement = enablement;
        this.auditMetadata = auditMetadata;

        this.credentials = credentials;
    }



    /*
    * STATIC FACTORIES
    */
    public static User reconstitute(
                @NonNull UserId id, 
                @NonNull Email email,
                @NonNull Username username, 
                @NonNull Birthday birthday,
                Instant lastLoginAt,
                @NonNull Enablement enablement,
                @NonNull AuditMetadata auditMetadata,
                @NonNull Credentials credentials
            ) {

        return new User(id, email, username, birthday, lastLoginAt, enablement, auditMetadata, credentials);
    }

    public static User register (
                @NonNull UserId id,
                @NonNull Email email, 
                @NonNull Username username,
                @NonNull Birthday birthday,
                @NonNull HashedPassword hashedPassword) {

        Enablement enablement = new Enablement();
        AuditMetadata auditMetadata = new AuditMetadata();
        Credentials credentials = Credentials.createNew(hashedPassword);
        User user = new User(
            id,
            email, 
            username, 
            birthday, 
            null, 
            enablement, 
            auditMetadata, 
            credentials
        );        
        
        UserRegisteredDomainEvent userRegisteredDomainEvent = new UserRegisteredDomainEvent(
            user.getId().getValue().toString(), 
            user.getEmail().getValue());
            
        user.getEvents().add(userRegisteredDomainEvent);

        return user;
    }

    

    /*
    * BEHAVIORS
    */

    // MODIFY SECURE STATUSES
    public void softDelete() {
        this.enablement = this.enablement.softDelete();
    }


    public void restore() {
        this.enablement = this.enablement.restore();
    }


    public void enable() {
        this.enablement = this.enablement.enable();
    }

    public void disable() {
        this.enablement = this.enablement.disable();
    }


    public void lock() {
        this.enablement = this.enablement.lock();
    }

    public void unlock() {
        this.enablement = this.enablement.unlock();
    }

    public void authenticate(@NonNull HashedPassword hashedPassword) {
        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(hashedPassword);
    }

    //RECHECK: may not a valid logic from the DDD perspective
    public void isAccessible() {
        this.enablement.validateAccessible();
    }


    //MODIFY USER INFO
    //RECHECK: implement usecase
    public void changeUsername(@NonNull Username newUsername, @NonNull HashedPassword hashedPassword) {
        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(hashedPassword);
        this.username = this.username.changeUsername(newUsername);
    }

    //RECHECK: implement usecase
    public void changeEmail(@NonNull Email newEmail, @NonNull HashedPassword hashedPassword) {
        this.enablement.validateAccessible();
        this.credentials.validateHashedPassword(hashedPassword);
        this.email = this.email.changeEmail(newEmail);
    }

    //RECHECK
    //public void changePhoneNumber(@NonNull PhoneNumber phoneNumber) {
        // push phone number change confirmation token domain event
    //}

    public void changePassword(@NonNull HashedPassword oldHashedPassword, @NonNull HashedPassword newHashedPassword) {
        this.enablement.validateAccessible();
        this.credentials = this.credentials.changePassword(oldHashedPassword, newHashedPassword);
    }

    //RECHECK: may not a valid logic from the DDD perspective
    public void refreshLastLogin() {
        this.lastLoginAt = Instant.now();
    }

    // try to move these methods into generic interfaces/classes and let our components implements them
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User other = (User) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}