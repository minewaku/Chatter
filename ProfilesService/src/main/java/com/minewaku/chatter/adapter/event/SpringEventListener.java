package com.minewaku.chatter.adapter.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.minewaku.chatter.adapter.db.postgresql.JpaOutboxRepository;
import com.minewaku.chatter.adapter.mapper.OutboxMapper;
import com.minewaku.chatter.adapter.messaging.publisher.EnrichedDomainEvent;
import com.minewaku.chatter.application.subcriber.CreateConfirmationTokenDomainEventSubcriber;
import com.minewaku.chatter.application.subcriber.SendConfirmationTokenDomainEventSubcriber;
import com.minewaku.chatter.domain.event.CreateConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;

@Component
public class SpringEventListener {

	private final JpaOutboxRepository jpaOutboxRepository;
	private final OutboxMapper outboxMapper;
	private final CreateConfirmationTokenDomainEventSubcriber createConfirmationTokenDomainEventSubcriber;
	private final SendConfirmationTokenDomainEventSubcriber sendConfirmationTokenDomainEventSubcriber;

	public SpringEventListener(
			JpaOutboxRepository jpaOutboxRepository,
			OutboxMapper outboxMapper,
			CreateConfirmationTokenDomainEventSubcriber createConfirmationTokenDomainEventSubcriber,
			SendConfirmationTokenDomainEventSubcriber sendConfirmationTokenDomainEventSubcriber) {
		this.jpaOutboxRepository = jpaOutboxRepository;
		this.outboxMapper = outboxMapper;
		this.createConfirmationTokenDomainEventSubcriber = createConfirmationTokenDomainEventSubcriber;
		this.sendConfirmationTokenDomainEventSubcriber = sendConfirmationTokenDomainEventSubcriber;
	}

	
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCreateConfirmationTokenDomainEvent(CreateConfirmationTokenDomainEvent event) {
		createConfirmationTokenDomainEventSubcriber.handle(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSendConfirmationTokenDomainEvent(SendConfirmationTokenDomainEvent event) {
		sendConfirmationTokenDomainEventSubcriber.handle(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserCreatedDomainEvent(UserCreatedDomainEvent event) {
		EnrichedDomainEvent<UserCreatedDomainEvent> enrichedEvent = outboxMapper
				.toUserCreatedDomainEvent(event.getCreatedUserDto());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAccountVerifiedDomainEvent(com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent> enrichedEvent = outboxMapper
				.toAccountVerifiedDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserSoftDeletedDomainEvent(com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent> enrichedEvent = outboxMapper
				.toUserSoftDeletedDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserRestoredDomainEvent(com.minewaku.chatter.domain.event.UserRestoredDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.UserRestoredDomainEvent> enrichedEvent = outboxMapper
				.toUserRestoredDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserHardDeletedDomainEvent(com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent> enrichedEvent = outboxMapper
				.toUserHardDeletedDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserLockedDomainEvent(com.minewaku.chatter.domain.event.UserLockedDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.UserLockedDomainEvent> enrichedEvent = outboxMapper
				.toUserLockedDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserUnlockedDomainEvent(com.minewaku.chatter.domain.event.UserUnlockedDomainEvent event) {
		EnrichedDomainEvent<com.minewaku.chatter.domain.event.UserUnlockedDomainEvent> enrichedEvent = outboxMapper
				.toUserUnlockedDomainEvent(event.getUserId());
		jpaOutboxRepository.save(outboxMapper.eventToEntity(enrichedEvent));
	}
}
