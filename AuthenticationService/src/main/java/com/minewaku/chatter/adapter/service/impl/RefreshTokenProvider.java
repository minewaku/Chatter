package com.minewaku.chatter.adapter.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;

@Service
public class RefreshTokenProvider implements RefreshTokenGenerator {

	private static final long REFRESH_TOKEN_EXPIRATION = 1800000; // 5 minutes (in milliseconds)
	
	@Override
	public RefreshToken generate(User user) {
		RefreshToken refreshToken = RefreshToken.createNew(UUID.randomUUID().toString(), 
				Duration.ofMillis(REFRESH_TOKEN_EXPIRATION), 
				Instant.now(),  
				user.getId());
			
		return refreshToken;
	}

}
