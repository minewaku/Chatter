package com.minewaku.chatter.application.service.auth;

import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.auth.LoginCommand;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.model.RefreshToken;
import com.minewaku.chatter.domain.model.Role;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.LoginUseCase;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;
import com.minewaku.chatter.domain.response.TokenResponse;
import com.minewaku.chatter.domain.service.auth.LoginDomainService;

public class LoginApplicationService implements LoginUseCase {
	
	private final CredentialsRepository credentialsRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final AccessTokenGenerator accessTokenGenerator;
	private final RefreshTokenGenerator refreshTokenGenerator;
	
	private final LoginDomainService loginDomainService;
	
	
	public LoginApplicationService(
		CredentialsRepository credentialsRepository,
		UserRepository userRepository,
		UserRoleRepository userRoleRepository,
		PasswordHasher passwordHasher,
		AccessTokenGenerator accessTokenGenerator,
		RefreshTokenGenerator refreshTokenGenerator) {
		
		this.credentialsRepository = credentialsRepository;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.accessTokenGenerator = accessTokenGenerator;
		this.refreshTokenGenerator = refreshTokenGenerator;
		
		loginDomainService = new LoginDomainService(passwordHasher);
	}
	
	
    @Override
    @Transactional
    public TokenResponse handle(LoginCommand command) {
		User user = userRepository.findByEmailAndIsDeletedFalse(command.getEmail())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		
		Credentials credentials = credentialsRepository.findByUserId(user.getId());
		loginDomainService.login(user, credentials, command.getPassword());
		
		Set<Role> roles = userRoleRepository.findRolesByUserIdAndIsDeletedFalse(user.getId());
		String accessToken = accessTokenGenerator.generate(user, roles);
		RefreshToken refreshToken = refreshTokenGenerator.generate(user);
		
		TokenResponse response = new TokenResponse(accessToken, refreshToken);
		
		return response;
	}
}
