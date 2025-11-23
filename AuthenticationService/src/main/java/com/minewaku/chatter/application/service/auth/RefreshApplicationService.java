package com.minewaku.chatter.application.service.auth;

import java.util.Set;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.RefreshUseCase;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;
import com.minewaku.chatter.domain.response.TokenResponse;

public class RefreshApplicationService implements RefreshUseCase {

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final AccessTokenGenerator accessTokenGenerator;
	
	
	public RefreshApplicationService(RefreshTokenRepository refreshTokenRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			RefreshTokenGenerator refreshTokenGenerator,
			AccessTokenGenerator accessTokenGenerator) {
		
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.accessTokenGenerator = accessTokenGenerator;
	}

	
    @Override
    public TokenResponse handle(String refreshToken) {
		RefreshToken existToken = refreshTokenRepository.findByToken(refreshToken)
			.orElseThrow(() -> new EntityNotFoundException("Refresh token does not exist"));
		
		User user = userRepository.findByIdAndIsDeletedFalse(existToken.getUserId())
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		
		Set<Role> roles = userRoleRepository.findRolesByUserIdAndIsDeletedFalse(user.getId());
		
		user.validateAccessible();
		
		existToken.revoke();
		refreshTokenRepository.revoke(existToken);
		
		RefreshToken newRefreshToken = refreshTokenGenerator.generate(user);
		String newAccessToken = accessTokenGenerator.generate(user, roles);
		
		TokenResponse tokenReponse = new TokenResponse(newAccessToken, newRefreshToken);
		return tokenReponse;
	}

}
