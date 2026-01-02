package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.service.user.HardDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.LockUserApplicationService;
import com.minewaku.chatter.application.service.user.RestoreUserApplicationService;
import com.minewaku.chatter.application.service.user.SoftDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.UnlockUserApplicationService;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;

@Configuration
class UserServiceConfig {
	
	@Bean
	SoftDeleteUserApplicationService softDeleteApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		return new SoftDeleteUserApplicationService(userRepository, storeEvent);
	}

	@Bean
	LockUserApplicationService lockApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		return new LockUserApplicationService(userRepository, storeEvent);
	}

	@Bean
	UnlockUserApplicationService unlockUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		return new UnlockUserApplicationService(userRepository, storeEvent);
	}

	@Bean
	RestoreUserApplicationService restoreUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		return new RestoreUserApplicationService(userRepository, storeEvent);
	}

	@Bean
	HardDeleteUserApplicationService hardDeleteUserApplicationService(UserRepository userRepository, StoreEvent storeEvent) {
		return new HardDeleteUserApplicationService(userRepository, storeEvent);
	}
}


