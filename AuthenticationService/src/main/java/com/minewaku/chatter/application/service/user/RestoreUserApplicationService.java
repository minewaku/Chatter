package com.minewaku.chatter.application.service.user;

import com.minewaku.chatter.domain.port.in.user.RestoreUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class RestoreUserApplicationService implements RestoreUserUseCase {
	
	private final UserRepository userRepository;
	
	public RestoreUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    @Override
    public Void handle(UserId userId) {
        userRepository.restoreById(userId);
        return null;
	}
}
