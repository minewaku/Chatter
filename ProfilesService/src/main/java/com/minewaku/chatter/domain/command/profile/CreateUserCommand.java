package com.minewaku.chatter.domain.command.profile;

import java.time.Instant;

import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CreateUserCommand {
	
	@NonNull
	private final UserId id;
	
	@NonNull
    private final Email email;
	
	@NonNull
    private final Username username;
	
	@NonNull
    private final Birthday birthday;
	
    private boolean enabled;
	
    private boolean locked;
	
    private boolean deleted;

    private Instant deletedAt;

    @NonNull
    private final AuditMetadata auditMetadata;
    
    public CreateUserCommand(@NonNull UserId id, @NonNull Email email, 
            @NonNull Username username, @NonNull Birthday birthday, 
            boolean enabled, boolean locked, boolean deleted, Instant deletedAt,
            @NonNull AuditMetadata auditMetadata) {
    	
        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.enabled = enabled;
        this.locked = locked;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.auditMetadata = auditMetadata;
    }
}