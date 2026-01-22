package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.service.user.CreateUserApplicationService;
import com.minewaku.chatter.application.service.user.HardDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.LockUserApplicationService;
import com.minewaku.chatter.application.service.user.RestoreUserApplicationService;
import com.minewaku.chatter.application.service.user.SoftDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.UnlockUserApplicationService;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;

@Configuration
public class UserServiceConfig {

	@Bean
	CreateUserApplicationService createUserApplicationService(UserRepository userRepository) {
		return new CreateUserApplicationService(userRepository);
	}
	
	@Bean
	SoftDeleteUserApplicationService softDeleteApplicationService(UserRepository userRepository) {
		return new SoftDeleteUserApplicationService(userRepository);
	}

	@Bean
	LockUserApplicationService lockApplicationService(UserRepository userRepository) {
		return new LockUserApplicationService(userRepository);
	}

	@Bean
	UnlockUserApplicationService unlockUserApplicationService(UserRepository userRepository) {
		return new UnlockUserApplicationService(userRepository);
	}

	@Bean
	RestoreUserApplicationService restoreUserApplicationService(UserRepository userRepository) {
		return new RestoreUserApplicationService(userRepository);
	}

	@Bean
	HardDeleteUserApplicationService hardDeleteUserApplicationService(UserRepository userRepository) {
		return new HardDeleteUserApplicationService(userRepository);
	}
}


