package com.minewaku.chatter.identityaccess.application.service.command.user.session;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.DomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventPublisher;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.domain.sync.SyncDomainEventQueue;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RefreshTokenCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RefreshUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor.TokenPayload;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionRefreshedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.RefreshDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.event.DomainEvent;

import io.github.resilience4j.retry.annotation.Retry;

public class RefreshApplicationService implements RefreshUseCase {

	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final AccessTokenGenerator accessTokenGenerator;
	private final RefreshTokenEncryptor refreshTokenEncryptor;

	private final RefreshDomainService refreshDomainService;

	private final DomainEventPublisher domainEventPublisher;


	public RefreshApplicationService(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
			RefreshTokenEncryptor refreshTokenEncryptor,
		
			RefreshDomainService refreshDomainService,

			SyncDomainEventQueue syncDomainEventQueue) {


		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.accessTokenGenerator = accessTokenGenerator;
		this.refreshTokenEncryptor = refreshTokenEncryptor;
		
		this.refreshDomainService = refreshDomainService;

		this.domainEventPublisher = new SyncDomainEventPublisher(syncDomainEventQueue);
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public TokenResponse handle(RefreshTokenCommand command) {

		TokenPayload tokenPayload = refreshTokenEncryptor.decrypt(command.refreshToken());
		SessionId sessionId = new SessionId(tokenPayload.sessionId());

		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new EntityNotFoundException("Refresh token not found"));

		User user = userRepository.findById(session.getUserId())
				.orElseThrow(() -> new EntityNotFoundException("User not found"));


		Session refreshedSession = refreshDomainService.handle(user, session, tokenPayload.generation());
		sessionRepository.save(refreshedSession);

		List<DomainEvent> filteredEvents = sessionRefreshedDomainEventFiltered(refreshedSession.getEvents());
        domainEventPublisher.publish(filteredEvents);

		String newAccessToken = accessTokenGenerator.generate(user.getId(), user.getEmail());
		String newRefreshToken = refreshTokenEncryptor.encrypt(refreshedSession);
		TokenResponse tokenReponse = new TokenResponse(newAccessToken, newRefreshToken);
		return tokenReponse;
	}

	private List<DomainEvent> sessionRefreshedDomainEventFiltered(List<DomainEvent> events) {
        return events.stream()
                .filter(event -> event instanceof SessionRefreshedDomainEvent)
                .toList();
    }
}

