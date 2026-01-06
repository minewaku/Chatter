package com.minewaku.chatter.application.service.auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.command.auth.RegisterCommand;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.Credentials;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.port.in.auth.RegisterUseCase;
import com.minewaku.chatter.domain.port.out.repository.CredentialsRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.port.out.service.IdGenerator;
import com.minewaku.chatter.domain.port.out.service.PasswordHasher;
import com.minewaku.chatter.domain.service.auth.CheckRegisterUserDomainService;
import com.minewaku.chatter.domain.value.HashedPassword;
import com.minewaku.chatter.domain.value.id.UserId;

public class RegisterApplicationService implements RegisterUseCase {

	private final CredentialsRepository credentialsRepository;
	private final UserRepository userRepository;
	private final PasswordHasher passwordHasher;
	private final IdGenerator idGenerator;

	private final QueueEventPublisher queueEventPublisher;
	private final StoreEventPublisher storeEventPublisher;

	private final CheckRegisterUserDomainService checkRegisterUserDomainService;

	public RegisterApplicationService(
			CredentialsRepository credentialsRepository,
			UserRepository userRepository,
			PasswordHasher passwordHasher,
			IdGenerator idGenerator,
			EmailSender emailSender,
			MessageQueue messageQueue,
			StoreEvent storeEvent,
			CheckRegisterUserDomainService checkRegisterUserDomainService) {

		this.credentialsRepository = credentialsRepository;
		this.userRepository = userRepository;
		this.passwordHasher = passwordHasher;
		this.idGenerator = idGenerator;

		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
		this.storeEventPublisher = new StoreEventPublisher(storeEvent);

		this.checkRegisterUserDomainService = checkRegisterUserDomainService;
	}

	@Override
	@Transactional
	public Void handle(RegisterCommand command) {

		userRepository.findByEmail(command.getEmail()).ifPresent(u -> {
			checkRegisterUserDomainService.handle(u);
		});

		UserId userId = new UserId(idGenerator.generate());

		User user = User.createNew(
				userId,
				command.getEmail(),
				command.getUsername(),
				command.getBirthday());

		User savedUser = userRepository.save(user);

		List<DomainEvent> filterEventsCreateUser = filterEventsCreateUser(user.getEvents());
		storeEventPublisher.publish(filterEventsCreateUser);

		HashedPassword hashedPassword = passwordHasher.hash(command.getPassword());
		Credentials credentials = Credentials.createNew(savedUser.getId(), hashedPassword);

		credentialsRepository.save(credentials);

		List<DomainEvent> filterEventsSendToken = filterEventsSendToken(credentials.getEvents());
		queueEventPublisher.publish(filterEventsSendToken);
		return null;
	}

	private List<DomainEvent> filterEventsSendToken(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(CreateConfirmationTokenDomainEvent.class))
				.collect(Collectors.toList());
	}

	private List<DomainEvent> filterEventsCreateUser(List<DomainEvent> events) {
		return events.stream()
				.filter(event -> event.getClass().equals(UserCreatedDomainEvent.class))
				.collect(Collectors.toList());
	}
}
