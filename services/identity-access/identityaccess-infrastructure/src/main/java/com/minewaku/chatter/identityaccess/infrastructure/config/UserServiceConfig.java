package com.minewaku.chatter.identityaccess.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.HardDeleteUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.LockUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.RestoreUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.SoftDeleteUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.user.usecase.UnlockUserUseCase;
import com.minewaku.chatter.identityaccess.application.service.command.user.HardDeleteUserApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.LockUserApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.RestoreUserApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.SoftDeleteUserApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.UnlockUserApplicationService;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

@Configuration
public class UserServiceConfig {
	
	@Bean
	SoftDeleteUserUseCase softDeleteUseCase(
                UserRepository userRepository, 
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore OutboxStore
			) {

		return new SoftDeleteUserApplicationService(
			userRepository, 
			uniqueStringIdGenerator, 
			OutboxStore);
	}

	@Bean
	LockUserUseCase lockUserUseCase(
                UserRepository userRepository, 
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore OutboxStore
			) {

		return new LockUserApplicationService(
			userRepository, 
			uniqueStringIdGenerator, 
			OutboxStore);
	}

	@Bean
	UnlockUserUseCase unlockUserUseCase(
                UserRepository userRepository, 
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore OutboxStore
			) {

		return new UnlockUserApplicationService(
			userRepository, 
			uniqueStringIdGenerator, 
			OutboxStore);
	}

	@Bean
	RestoreUserUseCase restoreUserUseCase(
                UserRepository userRepository, 
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore OutboxStore
			) {

		return new RestoreUserApplicationService(
			userRepository, 
			uniqueStringIdGenerator, 
			OutboxStore);
	}

	@Bean
	HardDeleteUserUseCase hardDeleteUserUseCase(
                UserRepository userRepository, 
				UniqueStringIdGenerator uniqueStringIdGenerator,
				OutboxStore OutboxStore
			) {

		return new HardDeleteUserApplicationService(
			userRepository, 
			uniqueStringIdGenerator, 
			OutboxStore);
	}
}
