package com.minewaku.chatter.identityaccess.application.service.command.user.credentials;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LoginCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LoginUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.PasswordHasher;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.credentials.HashedPassword;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.LoginDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import io.github.resilience4j.retry.annotation.Retry;


public class LoginApplicationService implements LoginUseCase {

	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final AccessTokenGenerator accessTokenGenerator;
    private final PasswordHasher passwordHasher;
	private final RefreshTokenEncryptor refreshTokenEncryptor;

	private final LoginDomainService loginDomainService;

	private final DomainEventPublisher domainEventPublisher;

	public LoginApplicationService(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
            PasswordHasher passwordHasher,
            RefreshTokenEncryptor refreshTokenEncryptor,
		
			LoginDomainService loginDomainService,
		
			SyncDomainEventQueue syncDomainEventQueue
		) {


		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.accessTokenGenerator = accessTokenGenerator;
        this.passwordHasher = passwordHasher;
		this.refreshTokenEncryptor = refreshTokenEncryptor;

		this.loginDomainService = loginDomainService;

		this.domainEventPublisher = new SyncDomainEventPublisher(syncDomainEventQueue);
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	//RECHECK: implement sending login notification email
	public TokenResponse handle(LoginCommand command) {

		User user = userRepository.findByEmail(command.email())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));

        HashedPassword hashedPassword = passwordHasher.hash(command.password());
		Session session = loginDomainService.handle(user, hashedPassword, command.deviceInfo(), null);
		sessionRepository.save(session);

		List<DomainEvent> filteredEvents = sessionCreatedDomainEventFiltered(session.getEvents());
        domainEventPublisher.publish(filteredEvents);

		String accessToken = accessTokenGenerator.generate(user.getId(), user.getEmail());
		String refreshToken = refreshTokenEncryptor.encrypt(session);

		TokenResponse response = new TokenResponse(accessToken, refreshToken);

		return response;
	}

	private List<DomainEvent> sessionCreatedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof SessionCreatedDomainEvent)
                .toList();
    }
}
