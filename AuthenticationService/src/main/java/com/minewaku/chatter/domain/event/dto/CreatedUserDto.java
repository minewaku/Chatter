package com.minewaku.chatter.domain.event.dto;

import com.minewaku.chatter.domain.value.AuditMetadata;
import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Username;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
public class CreatedUserDto {
	
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
    
    public CreatedUserDto(@NonNull UserId id, @NonNull Email email, 
            @NonNull Username username, @NonNull Birthday birthday, 
            @NonNull AuditMetadata auditMetadata) {
    	
        this.id = id;
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.isEnabled = false;
        this.isLocked = false;
        this.isDeleted = false;
        this.auditMetadata = auditMetadata;
    }
}
