package com.minewaku.chatter.application.service.auth;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.port.in.auth.LogoutUseCase;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;
import com.minewaku.chatter.domain.value.id.OpaqueToken;

public class LogoutApplicationService implements LogoutUseCase {
	
	private final RefreshTokenRepository refreshTokenRepository;
	
	
	public LogoutApplicationService(RefreshTokenRepository refreshTokenRepository) {
		this.refreshTokenRepository = refreshTokenRepository;
	}

	
    @Override
	@Transactional
    public Void handle(OpaqueToken opaqueToken) {
		RefreshToken existRefreshToken = refreshTokenRepository.findByToken(opaqueToken)
			.orElseThrow(() -> new EntityNotFoundException("Refresh token does not exist"));
		
		existRefreshToken.revoke();
        refreshTokenRepository.revoke(existRefreshToken);
        return null;
	}
}
