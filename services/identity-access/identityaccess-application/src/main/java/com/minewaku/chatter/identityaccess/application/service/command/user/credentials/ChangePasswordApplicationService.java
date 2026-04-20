package com.minewaku.chatter.identityaccess.application.service.command.user.credentials;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.ChangePasswordCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;

import io.github.resilience4j.retry.annotation.Retry;

@Service
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

		User user = userRepository.findById(command.userId())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));

		if(!user.changePassword(passwordHasher, command.password(), command.newPassword())) {
			userRepository.save(user);
		}
		return null;
	}
}

