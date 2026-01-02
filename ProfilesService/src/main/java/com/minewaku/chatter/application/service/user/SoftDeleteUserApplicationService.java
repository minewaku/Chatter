package com.minewaku.chatter.application.service.user;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.exception.EntityNotFoundException;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.user.SoftDeleteUserUseCase;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.value.id.UserId;

public class SoftDeleteUserApplicationService implements SoftDeleteUserUseCase {
	
	private final UserRepository userRepository;
	
	public SoftDeleteUserApplicationService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    @Override
	@Transactional
    public Void handle(UserId userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User does not exist"));
		user.softDelete();

        userRepository.softDelete(user);
        return null;
	}
}
