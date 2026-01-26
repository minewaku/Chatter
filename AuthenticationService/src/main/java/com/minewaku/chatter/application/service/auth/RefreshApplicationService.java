package com.minewaku.chatter.application.service.auth;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.DataInconsistencyException;
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
import com.minewaku.chatter.domain.value.id.OpaqueToken;

import io.github.resilience4j.retry.annotation.Retry;

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
	@Retry(name = "transientDataAccess")
	@Transactional
	public TokenResponse handle(OpaqueToken opaqueToken) {
		RefreshToken existToken = refreshTokenRepository.findByToken(opaqueToken)
				.orElseThrow(() -> new EntityNotFoundException("Refresh token does not exist"));

		existToken.checkRevokedOrExpired();

		User user = userRepository.findById(existToken.getUserId())
			.orElseThrow(() -> new DataInconsistencyException(
				String.format(
					"Data Integrity Violation: RefreshToken '%s' exists, but associated User '%s' is missing from database.",
					existToken.getToken(),
					existToken.getUserId()
				)
        ));

		user.validateAccessible();

		Set<Role> roles = userRoleRepository.findRolesByUserIdAndIsDeletedFalse(user.getId());

		OpaqueToken newOpaqueToken = new OpaqueToken(refreshTokenGenerator.generate());
		RefreshToken newRefreshToken = RefreshToken.createNew(newOpaqueToken, null, user.getId());
		refreshTokenRepository.save(newRefreshToken);

		existToken.replace(newRefreshToken);
		refreshTokenRepository.revoke(existToken);

		String newAccessToken = accessTokenGenerator.generate(user, roles);

		TokenResponse tokenReponse = new TokenResponse(newAccessToken, newRefreshToken);
		return tokenReponse;
	}
}
