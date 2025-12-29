package com.minewaku.chatter.application.service.auth;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.command.auth.ChangePasswordCommand;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.ChangePasswordUseCase;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.service.auth.PasswordSecurityDomainService;
import com.minewaku.chatter.domain.value.HashedPassword;

public class ChangePasswordApplicationService implements ChangePasswordUseCase {

	private final CredentialsRepository credentialsRepository;
	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	private final PasswordSecurityDomainService passwordSecurityDomainService;

	public ChangePasswordApplicationService(
			CredentialsRepository credentialsRepository,
			UserRepository userRepository,
			PasswordHasher passwordHasher) {

		this.credentialsRepository = credentialsRepository;
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;

		passwordSecurityDomainService = new PasswordSecurityDomainService(passwordHasher);
	}

	@Override
	@Transactional
	public Void handle(ChangePasswordCommand command) {
		User user = userRepository.findByEmail(command.getEmail())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.validateAccessible();

		Credentials credentials = credentialsRepository.findById(user.getId()).orElseThrow(
				() -> new EntityNotFoundException("This login method for this user does not exist"));

		
		
		passwordSecurityDomainService.validateCredentials(credentials, command.getPassword());

		HashedPassword newHashedPassword = passwordHasher.hash(command.getNewPassword());

		passwordSecurityDomainService.validatePasswordReuse(credentials, command.getNewPassword());

		credentials.changePassword(newHashedPassword);
		credentialsRepository.save(credentials);
		return null;
	}
}
