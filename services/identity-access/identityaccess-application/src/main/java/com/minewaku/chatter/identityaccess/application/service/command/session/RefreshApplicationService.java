package com.minewaku.chatter.identityaccess.application.service.command.session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.RefreshTokenCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.RefreshUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor.TokenPayload;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.SessionId;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.RefreshDomainService;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class RefreshApplicationService implements RefreshUseCase {

	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final AccessTokenGenerator accessTokenGenerator;
	private final RefreshTokenEncryptor refreshTokenEncryptor;

	private final RefreshDomainService refreshDomainService;


	public RefreshApplicationService(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
			RefreshTokenEncryptor refreshTokenEncryptor,
		
			RefreshDomainService refreshDomainService) {


		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.accessTokenGenerator = accessTokenGenerator;
		this.refreshTokenEncryptor = refreshTokenEncryptor;
		
		this.refreshDomainService = refreshDomainService;
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
		if(refreshedSession != null) {
			sessionRepository.save(refreshedSession);
		}

		String newAccessToken = accessTokenGenerator.generate(user.getId(), user.getEmail());
		String newRefreshToken = refreshTokenEncryptor.encrypt(refreshedSession);
		TokenResponse tokenReponse = new TokenResponse(newAccessToken, newRefreshToken);
		return tokenReponse;
	}

}

