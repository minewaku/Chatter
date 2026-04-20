package com.minewaku.chatter.identityaccess.application.service.command.user.credentials;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.exception.EntityNotFoundException;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.command.LoginCommand;
import com.minewaku.chatter.identityaccess.application.port.inbound.command.auth.usecase.LoginUseCase;
import com.minewaku.chatter.identityaccess.application.port.inbound.shared.response.TokenResponse;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.AccessTokenGenerator;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.RefreshTokenEncryptor;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.model.Session;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.repository.SessionRepository;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.User;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.repository.UserRepository;
import com.minewaku.chatter.identityaccess.domain.service.LoginDomainService;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.service.PasswordHasher;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class LoginApplicationService implements LoginUseCase {

	private final UserRepository userRepository;
	private final SessionRepository sessionRepository;
	private final AccessTokenGenerator accessTokenGenerator;
    private final PasswordHasher passwordHasher;
	private final RefreshTokenEncryptor refreshTokenEncryptor;

	private final LoginDomainService loginDomainService;

	public LoginApplicationService(
			UserRepository userRepository,
			SessionRepository sessionRepository,
			AccessTokenGenerator accessTokenGenerator,
            PasswordHasher passwordHasher,
            RefreshTokenEncryptor refreshTokenEncryptor,
		
			LoginDomainService loginDomainService
		) {


		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.accessTokenGenerator = accessTokenGenerator;
        this.passwordHasher = passwordHasher;
		this.refreshTokenEncryptor = refreshTokenEncryptor;

		this.loginDomainService = loginDomainService;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	//RECHECK: implement sending login notification email
	public TokenResponse handle(LoginCommand command) {

		User user = userRepository.findByEmail(command.email())
				.orElseThrow(() -> new EntityNotFoundException("User does not exist"));

		Session session = loginDomainService.handle(passwordHasher, user, command.password(), command.deviceInfo(), null);
		sessionRepository.save(session);

		String accessToken = accessTokenGenerator.generate(user.getId(), user.getEmail());
		String refreshToken = refreshTokenEncryptor.encrypt(session);

		TokenResponse response = new TokenResponse(accessToken, refreshToken);

		return response;
	}
}
