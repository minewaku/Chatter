package com.minewaku.chatter.identityaccess.infrastructure.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserEnabledIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserHardDeletedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserLockedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserRegisteredIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserRestoredIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserSoftDeletedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.UserUnlockedIntegrationEvent;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.ConfirmationTokenCreatedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.ConfirmationTokenVerifiedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.SessionCreatedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.SessionRefreshedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.UserRegisteredDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.session.event.SessionRefreshedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.JpaOutboxRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.mapper.OutboxMapper;

@Component
public class SpringEventListener {

	private final JpaOutboxRepository jpaOutboxRepository;
	private final OutboxMapper outboxMapper;

	private final ConfirmationTokenCreatedDomainEventSubcriber confirmationTokenCreatedDomainEventSubcriber;
    private final ConfirmationTokenVerifiedDomainEventSubcriber confirmationTokenVerifiedDomainEventSubcriber;
    private final SessionCreatedDomainEventSubcriber sessionCreatedDomainEventSubcriber;
    private final SessionRefreshedDomainEventSubcriber sessionRefreshedDomainEventSubcriber;
    private final UserRegisteredDomainEventSubcriber userRegisteredDomainEventSubcriber;


	public SpringEventListener(
			JpaOutboxRepository jpaOutboxRepository,
			OutboxMapper outboxMapper,

			ConfirmationTokenCreatedDomainEventSubcriber confirmationTokenCreatedDomainEventSubcriber,
			ConfirmationTokenVerifiedDomainEventSubcriber confirmationTokenVerifiedDomainEventSubcriber,
            SessionCreatedDomainEventSubcriber sessionCreatedDomainEventSubcriber,
            SessionRefreshedDomainEventSubcriber sessionRefreshedDomainEventSubcriber,
            UserRegisteredDomainEventSubcriber userRegisteredDomainEventSubcriber) {


		this.jpaOutboxRepository = jpaOutboxRepository;
		this.outboxMapper = outboxMapper;

		this.confirmationTokenCreatedDomainEventSubcriber = confirmationTokenCreatedDomainEventSubcriber;
		this.confirmationTokenVerifiedDomainEventSubcriber = confirmationTokenVerifiedDomainEventSubcriber;
        this.sessionCreatedDomainEventSubcriber = sessionCreatedDomainEventSubcriber;
        this.sessionRefreshedDomainEventSubcriber = sessionRefreshedDomainEventSubcriber;
        this.userRegisteredDomainEventSubcriber = userRegisteredDomainEventSubcriber;
	}


    //SyncDomainEventSubcribers 
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCreateConfirmationTokenDomainEvent(ConfirmationTokenCreatedDomainEvent event) {
		confirmationTokenCreatedDomainEventSubcriber.handle(event);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSendConfirmationTokenDomainEvent(ConfirmationTokenVerifiedDomainEvent event) {
		confirmationTokenVerifiedDomainEventSubcriber.handle(event);
	}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSessionCreatedDomainEvent(SessionCreatedDomainEvent event) {
		sessionCreatedDomainEventSubcriber.handle(event);
	}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSessionRefreshedDomainEvent(SessionRefreshedDomainEvent event) {
		sessionRefreshedDomainEventSubcriber.handle(event);
	}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserRegisteredDomainEvent(UserRegisteredDomainEvent event) {
		userRegisteredDomainEventSubcriber.handle(event);
	}



    //IntegrationEventSubcribers - Outbox Pattern
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onUserEnabledIntegrationEvent(IntegrationEventWrapper<UserEnabledIntegrationEvent> event) {
		jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
	}

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserHardDeletedIntegrationEvent(IntegrationEventWrapper<UserHardDeletedIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserLockedIntegrationEvent(IntegrationEventWrapper<UserLockedIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserRegisteredIntegrationEvent(IntegrationEventWrapper<UserRegisteredIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserRestoredIntegrationEvent(IntegrationEventWrapper<UserRestoredIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserSoftDeletedIntegrationEvent(IntegrationEventWrapper<UserSoftDeletedIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserUnlockedIntegrationEvent(IntegrationEventWrapper<UserUnlockedIntegrationEvent> event) {
        jpaOutboxRepository.save(outboxMapper.integrationEventWrapperToEntity(event));
    }
}
