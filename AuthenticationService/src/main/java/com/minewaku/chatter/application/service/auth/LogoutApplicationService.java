package com.minewaku.chatter.application.service.auth;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.port.in.auth.LogoutUseCase;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;

public class LogoutApplicationService implements LogoutUseCase {
	
	private final RefreshTokenRepository refreshTokenRepository;
	
	
	public LogoutApplicationService(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	
    @Override
	@Transactional
    public Void handle(String refreshToken) {
		RefreshToken existRefreshToken = refreshTokenRepository.findByToken(refreshToken)
			.orElseThrow(() -> new EntityNotFoundException("Refresh token does not exist"));
		
		existRefreshToken.revoke();
        refreshTokenRepository.revoke(existRefreshToken);
        return null;
	}
}
