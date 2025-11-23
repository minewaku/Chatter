package com.minewaku.chatter.application.service.user;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.UnlockUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class UnlockUserApplicationService implements UnlockUserUseCase {
	
	private final UserRepository userRepository;
	
	
	public UnlockUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	
    @Override
    public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found")); 
		
		user.unlock();
        userRepository.update(user);
        return null;
	}
}
