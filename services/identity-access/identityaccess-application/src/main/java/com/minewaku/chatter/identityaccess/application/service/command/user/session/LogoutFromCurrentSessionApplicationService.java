package com.minewaku.chatter.identityaccess.application.service.command.user.session;


import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LogoutCurrentSessionCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LogoutCurrentSessionUseCase;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor.TokenPayload;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;

import io.github.resilience4j.retry.annotation.Retry;

public class LogoutFromCurrentSessionApplicationService implements LogoutCurrentSessionUseCase {
	
	private final SessionRepository sessionRepository;
	private final RefreshTokenEncryptor	refreshTokenEncryptor;
	
	
	public LogoutFromCurrentSessionApplicationService(
				SessionRepository sessionRepository,
				RefreshTokenEncryptor refreshTokenEncryptor) {

		this.sessionRepository = sessionRepository;
		this.refreshTokenEncryptor = refreshTokenEncryptor;
	}

	
    @Override
	@Retry(name = "transientDataAccess")
	@Transactional
    public Void handle(LogoutCurrentSessionCommand command) {

		TokenPayload tokenPayload = refreshTokenEncryptor.decrypt(command.refreshToken());
		SessionId sessionId = new SessionId(tokenPayload.sessionId());

		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new EntityNotFoundException("Session not found"));
		
		session.logoutFromCurrentSession(command.currentSessionId());
		sessionRepository.save(session);

        return null;
	}
}
