package com.minewaku.chatter.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.dto.CreatedUserDto;
import com.minewaku.chatter.domain.exception.ForbiddenAccessException;
import com.minewaku.chatter.domain.exception.ResourceNotFoundException;
import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class User {
	
	@NonNull
	private final UserId id;
	
	@NonNull
    private final Email email;
	
	@NonNull
    private final Username username;
	
	@NonNull
    private final Birthday birthday;
	
    @Setter
    private boolean isEnabled;
	
    @Setter
    private boolean isLocked;
	
	@Setter
    private boolean isDeleted;

    @NonNull
    private final AuditMetadata auditMetadata;
    
    private Instant deletedAt;
    
    @NonNull
    private final List<DomainEvent> events = new ArrayList<DomainEvent>();

    
    
    // Private constructor
    private User(@NonNull UserId id, @NonNull Email email, 
            @NonNull Username username, @NonNull Birthday birthday, @NonNull AuditMetadata auditMetadata
            , boolean isEnabled, boolean isLocked, boolean isDeleted, Instant deletedAt) {
    	
        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.isEnabled = isEnabled;
        this.isLocked = isLocked;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.auditMetadata = auditMetadata;
    }

    
    
    // Static factory for loading existing data
    public static User reconstitute(@NonNull UserId id, @NonNull Email email, 
            @NonNull Username username, @NonNull Birthday birthday, 
            @NonNull AuditMetadata auditMetadata, 
            boolean isEnabled, boolean isLocked, boolean isDeleted, Instant deletedAt) {
    	
        return new User(id, email, username, birthday, auditMetadata, isEnabled, isLocked, isDeleted, deletedAt);
    }

    // Static factory for creating new data
    public static User createNew(@NonNull UserId id, @NonNull Email email, @NonNull Username username, @NonNull Birthday birthday) {
        User user = new User(id, email, username, birthday, new AuditMetadata(), false, false, false, null);
        CreatedUserDto createdUserDto = new CreatedUserDto(id, email, username, birthday, user.getAuditMetadata());
        
        UserCreatedDomainEvent userCreatedDomainEvent = new UserCreatedDomainEvent(createdUserDto);
        user.getEvents().add(userCreatedDomainEvent);
        return user;
    }
    
    
    
    public void validateAccessible() {
    	if(this.isDeleted) {
    		throw new ResourceNotFoundException("User does not exist");
    	}
    	if(this.isLocked) {
    		throw new ForbiddenAccessException("User is locked");
    	}
    	if(!this.isEnabled) {
    		throw new ForbiddenAccessException("User is unabled");
    	}
    }

    public void checkForDisable() {
        if(this.isEnabled) {
        	throw new ForbiddenAccessException("User is already enabled");
        }           
    }
    
    public void hardDeleted() {
    	UserHardDeletedDomainEvent userHardDeletedDomainEvent = new UserHardDeletedDomainEvent(this.id);
    	this.getEvents().add(userHardDeletedDomainEvent);
    }
    

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        UserSoftDeletedDomainEvent userSoftDeletedDomainEvent = new UserSoftDeletedDomainEvent(this.id);
        this.events.add(userSoftDeletedDomainEvent);
    }
    
    
    public void enable() {
        this.isEnabled = true;
        this.auditMetadata.markUpdated();
        AccountVerifiedDomainEvent accountVerifiedDomainEvent = new AccountVerifiedDomainEvent(this.id);
        this.events.add(accountVerifiedDomainEvent);
    }
    
    
    public void lock() {
        this.isLocked = true;
        this.auditMetadata.markUpdated();
        UserLockedDomainEvent lockUserDomainEvent = new UserLockedDomainEvent(this.id);
        this.events.add(lockUserDomainEvent);
    }
    

    public void unlock() {
        this.isLocked = false;
        this.auditMetadata.markUpdated();
        UserUnlockedDomainEvent unlockUserDomainEvent = new UserUnlockedDomainEvent(this.id);
        this.events.add(unlockUserDomainEvent);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
