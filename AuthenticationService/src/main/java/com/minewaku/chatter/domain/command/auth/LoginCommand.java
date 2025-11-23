package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.Password;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class LoginCommand {
	
	@NonNull
    private final Email email;
	
	@NonNull
    private final Password password;

    public LoginCommand(@NonNull Email email, @NonNull Password password) {
        this.email = email;
        this.password = password;
    }
}
