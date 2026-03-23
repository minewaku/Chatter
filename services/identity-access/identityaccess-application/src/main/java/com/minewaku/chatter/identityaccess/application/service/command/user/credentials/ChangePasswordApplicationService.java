package com.minewaku.chatter.identityaccess.application.service.command.user.credentials;


import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.ChangePasswordCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.PasswordHasher;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class ChangePasswordApplicationService implements ChangePasswordUseCase {

	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;

	public ChangePasswordApplicationService(
			UserRepository userRepository,
			PasswordHasher passwordHasher) {

		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public Void handle(ChangePasswordCommand command) {

		User user = userRepository.findByEmail(command.email())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
				
        HashedPassword oldHashedPassword = passwordHasher.hash(command.password());
        HashedPassword newHashedPassword = passwordHasher.hash(command.newPassword());

		user.changePassword(oldHashedPassword, newHashedPassword);
		userRepository.save(user);
		return null;
	}
}

