package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.value.Birthday;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Password;
import com.minewaku.chatter.domain.value.Username;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class RegisterCommand {
	
	@NonNull
    private final Email email;
	
	@NonNull
    private final Username username;
	
	@NonNull
    private final Birthday birthday;
	
	@NonNull
    private final Password password;

    public RegisterCommand(@NonNull Email email, 
    		@NonNull Username username, @NonNull Birthday birthday, 
    		@NonNull Password password) {
    	
        this.email = email;
        this.username = username;
        this.birthday = birthday;
        this.password = password;
    }
}
