package com.minewaku.chatter.application.service.user;

import com.minewaku.chatter.domain.port.in.user.SoftDeleteUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class SoftDeleteUserApplicationService implements SoftDeleteUserUseCase {
	
	private final UserRepository userRepository;
	
	public SoftDeleteUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    @Override
    public Void handle(UserId userId) {
        userRepository.softDeleteById(userId);
        return null;
	}
}
