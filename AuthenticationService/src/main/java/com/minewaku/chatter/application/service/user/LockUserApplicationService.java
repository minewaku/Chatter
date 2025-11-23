package com.minewaku.chatter.application.service.user;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.LockUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class LockUserApplicationService implements LockUserUseCase {
	
	private final UserRepository userRepository;
	
	
	public LockUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	
    @Override
    public Void handle(UserId userId) {
		User user = userRepository.findByIdAndIsDeletedFalse(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found")); 
		
		user.lock();
        userRepository.update(user);
        return null;
	}
}
