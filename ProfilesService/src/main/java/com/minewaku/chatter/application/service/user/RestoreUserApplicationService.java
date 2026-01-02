package com.minewaku.chatter.application.service.user;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.RestoreUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class RestoreUserApplicationService implements RestoreUserUseCase {
	
	private final UserRepository userRepository;
	
	public RestoreUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    @Override
	@Transactional
    public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.restore();

        userRepository.restore(user);
        return null;
	}
}
