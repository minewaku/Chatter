package com.minewaku.chatter.application.service.user;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.port.in.user.HardDeleteUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class HardDeleteUserApplicationService implements HardDeleteUserUseCase {
	
	private final UserRepository userRepository;
	
	public HardDeleteUserApplicationService(
			UserRepository userRepository) {
		
		this.userRepository = userRepository;
	}
	
	
    @Override
	@Transactional
    public Void handle(UserId userId) {
        userRepository.hardDeleteById(userId);
        return null;
	}
}
