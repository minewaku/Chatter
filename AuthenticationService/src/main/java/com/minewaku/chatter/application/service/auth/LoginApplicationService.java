package com.minewaku.chatter.application.service.auth;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.auth.LoginCommand;
import com.minewaku.chatter.domain.exception.InvalidCredentialsException;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.LoginUseCase;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;
import com.minewaku.chatter.domain.response.TokenResponse;
import com.minewaku.chatter.domain.service.auth.PasswordSecurityDomainService;
import com.minewaku.chatter.domain.value.id.OpaqueToken;

public class LoginApplicationService implements LoginUseCase {

	private final CredentialsRepository credentialsRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;

	private final AccessTokenGenerator accessTokenGenerator;
	private final RefreshTokenGenerator refreshTokenGenerator;

	private final PasswordSecurityDomainService passwordSecurityDomainService;

	public LoginApplicationService(
			CredentialsRepository credentialsRepository,
			RefreshTokenRepository refreshTokenRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			PasswordHasher passwordHasher,
			AccessTokenGenerator accessTokenGenerator,
			RefreshTokenGenerator refreshTokenGenerator) {

		this.credentialsRepository = credentialsRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.accessTokenGenerator = accessTokenGenerator;
		this.refreshTokenGenerator = refreshTokenGenerator;

		passwordSecurityDomainService = new PasswordSecurityDomainService(passwordHasher);
	}

	@Override
	@Transactional
	public TokenResponse handle(LoginCommand command) {
		User user = userRepository.findByEmail(command.email())
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.validateAccessible();

		Credentials credentials = credentialsRepository.findById(user.getId())
			.orElseThrow(() -> new InvalidCredentialsException("This login method for this user does not exist"));

		passwordSecurityDomainService.validateCredentials(credentials, command.password());

		Set<Role> roles = userRoleRepository.findRolesByUserIdAndIsDeletedFalse(user.getId());
		String accessToken = accessTokenGenerator.generate(user, roles);

		OpaqueToken opaqueToken = new OpaqueToken(refreshTokenGenerator.generate());
		RefreshToken refreshToken = RefreshToken.createNew(opaqueToken, null, user.getId());
		refreshTokenRepository.save(refreshToken);

		TokenResponse response = new TokenResponse(accessToken, refreshToken);

		return response;
	}
}
