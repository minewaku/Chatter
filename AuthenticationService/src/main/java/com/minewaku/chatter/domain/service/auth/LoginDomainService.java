package com.minewaku.chatter.domain.service.auth;

import com.minewaku.chatter.domain.exception.ForbiddenAccessException;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.value.Password;

public class LoginDomainService {
	
	private PasswordHasher passwordHasher;
	
	public LoginDomainService(PasswordHasher passwordHasher) {
		this.passwordHasher = passwordHasher;
	}
	
	
	public void login(User user, Credentials credentials, Password password) {
		if (!passwordHasher.matches(password, credentials.getHashedPassword())) {
            throw new ForbiddenAccessException("Invalid credentials");
        }
		
		user.validateAccessible();
	}
}
