package com.minewaku.chatter.adapter.config.impl.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.service.auth.ChangePasswordApplicationService;
import com.minewaku.chatter.application.service.auth.LoginApplicationService;
import com.minewaku.chatter.application.service.auth.LogoutApplicationService;
import com.minewaku.chatter.application.service.auth.RefreshApplicationService;
import com.minewaku.chatter.application.service.auth.RegisterApplicationService;
import com.minewaku.chatter.application.service.auth.ResendConfirmationTokenApplicationService;
import com.minewaku.chatter.application.service.auth.VerifyConfirmationTokenApplicationService;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.port.out.repository.RefreshTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRoleRepository;
import com.minewaku.chatter.domain.port.out.service.AccessTokenGenerator;
import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.port.out.service.IdGenerator;
import com.minewaku.chatter.domain.port.out.service.KeyGenerator;
import com.minewaku.chatter.domain.port.out.service.LinkGenerator;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.port.out.service.RefreshTokenGenerator;

@Configuration
class AuthServiceConfig {
	
    @Bean
	RegisterApplicationService registerApplicationService(
		CredentialsRepository credentialsRepository,
		UserRepository userRepository,
		PasswordHasher passwordHasher,
		IdGenerator idGenerator,
		LinkGenerator linkGenerator,
		EmailSender emailSender,
		MessageQueue messageQueue,
		StoreEvent storeEvent
	) {
		return new RegisterApplicationService(
				credentialsRepository,
				userRepository,
				passwordHasher,
				idGenerator,
				linkGenerator,
				emailSender,
				messageQueue,
				storeEvent);
	}

	@Bean
	LoginApplicationService loginApplicationService(
			CredentialsRepository credentialsRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			PasswordHasher passwordHasher,
			AccessTokenGenerator accessTokenGenerator,
			RefreshTokenGenerator refreshTokenGenerator
	) {
		return new LoginApplicationService(
				credentialsRepository,
				userRepository,
				userRoleRepository,
				passwordHasher,
				accessTokenGenerator,
				refreshTokenGenerator);
	}

	@Bean
	ChangePasswordApplicationService changePasswordApplicationService(
			CredentialsRepository credentialsRepository,
			UserRepository userRepository,
			PasswordHasher passwordHasher
	) {
		return new ChangePasswordApplicationService(
				credentialsRepository,
				userRepository,
				passwordHasher);
	}

	@Bean
	LogoutApplicationService logoutApplicationService(
			RefreshTokenRepository refreshTokenRepository
	) {
		return new LogoutApplicationService(refreshTokenRepository);
	}

	@Bean
	RefreshApplicationService refreshApplicationService(
			RefreshTokenRepository refreshTokenRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			RefreshTokenGenerator refreshTokenGenerator,
			AccessTokenGenerator accessTokenGenerator
	) {
		return new RefreshApplicationService(
				refreshTokenRepository,
				userRepository,
				userRoleRepository,
				refreshTokenGenerator,
				accessTokenGenerator);
	}

	@Bean
	VerifyConfirmationTokenApplicationService verifyConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository,
			UserRepository userRepository,
			MessageQueue messageQueue
	) {
		return new VerifyConfirmationTokenApplicationService(
				confirmationTokenRepository,
				userRepository,
				messageQueue);
	}

	@Bean
	ResendConfirmationTokenApplicationService resendConfirmationTokenApplicationService(
			ConfirmationTokenRepository confirmationTokenRepository, 
			UserRepository userRepository, 
			KeyGenerator keyGenerator, 
			MessageQueue messageQueue
	) {
		return new ResendConfirmationTokenApplicationService(
				confirmationTokenRepository, 
				userRepository, 
				keyGenerator, 
				messageQueue);
	}
}
