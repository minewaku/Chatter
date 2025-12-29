package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.service.confirmation_token.CreateConfirmationTokenApplicationService;
import com.minewaku.chatter.application.service.confirmation_token.DeleteConfirmationTokenApplicationService;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.ConfirmationTokenGenerator;

@Configuration
class ConfirmationTokenServiceConfig {

	@Bean
	CreateConfirmationTokenApplicationService createConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			ConfirmationTokenGenerator keyGenerator,
			MessageQueue messageQueue) {
		return new CreateConfirmationTokenApplicationService(
				confirmationTokenRepository,
				userRepository,
				keyGenerator,
				messageQueue);
	}

	@Bean
	DeleteConfirmationTokenApplicationService deleteConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository
	) {
		return new DeleteConfirmationTokenApplicationService(confirmationTokenRepository);
	}
}
