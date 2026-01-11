package com.minewaku.chatter.domain.event.dto;

import java.time.Instant;

import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class CreateUserDto {
	
	@NonNull
	private final UserId id;
	
	@NonNull
    private final Email email;
	
	@NonNull
    private final Username username;
	
	@NonNull
    private final Birthday birthday;
	
    @Setter
    private boolean enabled;
	
    @Setter
    private boolean locked;
	
	@Setter
    private boolean deleted;

    
    private Instant deletedAt;

    @NonNull
    private final AuditMetadata auditMetadata;
    
    public CreateUserDto(@NonNull UserId id, @NonNull Email email, 
            @NonNull Username username, @NonNull Birthday birthday, 
            @NonNull AuditMetadata auditMetadata) {
    	
        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.enabled = false;
        this.locked = false;
        this.deleted = false;
        this.deletedAt = null;
        this.auditMetadata = auditMetadata;
    }
}
