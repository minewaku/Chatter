package com.minewaku.chatter.identityaccess.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.ChangePasswordUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LoginUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RefreshUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RegisterUserUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.ResendConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.confirmationtoken.usecase.VerifyConfirmationTokenUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.PasswordHasher;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.application.service.command.confirmationtoken.ResendConfirmationTokenApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.confirmationtoken.VerifyConfirmationTokenApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.RegisterUserApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.credentials.ChangePasswordApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.credentials.LoginApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.session.LogoutFromCurrentSessionApplicationService;
import com.minewaku.chatter.identityaccess.application.service.command.user.session.RefreshApplicationService;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.LoginDomainService;
import com.minewaku.chatter.identityaccess.domain.service.RefreshDomainService;
import com.minewaku.chatter.identityaccess.domain.service.ResendConfirmationTokenDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.TimeBasedIdGenerator;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.UniqueStringIdGenerator;

@Configuration
public class AuthServiceConfig {
    
    @Bean
	RegisterUserUseCase registerUserUseCase(
			UserRepository userRepository,

			PasswordHasher passwordHasher,
			TimeBasedIdGenerator timeBasedIdGenerator,
			UniqueStringIdGenerator uniqueStringIdGenerator,

			SyncDomainEventQueue syncDomainEventQueue,
			OutboxStore outboxStore) {

		return new RegisterUserApplicationService(
				userRepository,
				passwordHasher,
				timeBasedIdGenerator,
				uniqueStringIdGenerator,
				syncDomainEventQueue,
				outboxStore);
	}

	@Bean
	LoginUseCase loginUseCase(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
            PasswordHasher passwordHasher,
            RefreshTokenEncryptor refreshTokenEncryptor,
		
			LoginDomainService loginDomainService,
		
			SyncDomainEventQueue syncDomainEventQueue)  {

		return new LoginApplicationService(
				userRepository,
				sessionRepository,
				accessTokenGenerator,
				passwordHasher,
                refreshTokenEncryptor,
                loginDomainService,
                syncDomainEventQueue);
	}

	@Bean
	ChangePasswordUseCase changePasswordUseCase(
			UserRepository userRepository,
			PasswordHasher passwordHasher) {

		return new ChangePasswordApplicationService(
				userRepository,
				passwordHasher);
	}

	@Bean
	LogoutCurrentSessionUseCase logoutUseCase(
				SessionRepository sessionRepository,
				RefreshTokenEncryptor refreshTokenEncryptor) {

		return new LogoutFromCurrentSessionApplicationService(
                sessionRepository, 
                refreshTokenEncryptor);
	}

	@Bean
	RefreshUseCase refreshUseCase(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
			RefreshTokenEncryptor refreshTokenEncryptor,
		
			RefreshDomainService refreshDomainService,

			SyncDomainEventQueue syncDomainEventQueue) {

		return new RefreshApplicationService(
				userRepository,
				sessionRepository,
				accessTokenGenerator,
				refreshTokenEncryptor,
                refreshDomainService,
                syncDomainEventQueue);
	}

	@Bean
	VerifyConfirmationTokenUseCase verifyConfirmationTokenUseCase(
			ConfirmationTokenRepository confirmationTokenRepository,
			SyncDomainEventQueue syncDomainEventQueue) {	

		return new VerifyConfirmationTokenApplicationService(
				confirmationTokenRepository,
				syncDomainEventQueue);
	}

	@Bean
	ResendConfirmationTokenUseCase resendConfirmationTokenUseCase(
                ConfirmationTokenRepository confirmationTokenRepository,
                UserRepository userRepository,

                SyncDomainEventQueue syncDomainEventQueue,
            
                ResendConfirmationTokenDomainService resendConfirmationTokenDomainService) {

		return new ResendConfirmationTokenApplicationService(
				confirmationTokenRepository,
				userRepository,
				syncDomainEventQueue,
				resendConfirmationTokenDomainService);
	}
}
