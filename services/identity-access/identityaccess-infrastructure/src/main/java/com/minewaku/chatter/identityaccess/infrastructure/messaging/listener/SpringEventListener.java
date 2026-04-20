package com.minewaku.chatter.identityaccess.infrastructure.messaging.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain.ConfirmationTokenVerifiedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain.UserRegisteredDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.application.messaging.subcriber.domain.UserSoftDeletedDomainEventSubcriber;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenVerifiedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserRegisteredDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.JdbcOutboxRepository;
import com.minewaku.chatter.identityaccess.infrastructure.persistence.postgresql.mapper.JdbcOutboxMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class SpringEventListener {

	private final JdbcOutboxRepository JdbcOutboxRepository;
	private final JdbcOutboxMapper jdbcOutboxMapper;

    private final ConfirmationTokenVerifiedDomainEventSubcriber confirmationTokenVerifiedDomainEventSubcriber;
    private final UserRegisteredDomainEventSubcriber userRegisteredDomainEventSubcriber;
	private final UserSoftDeletedDomainEventSubcriber userSoftDeletedDomainEventSubcriber;

	public SpringEventListener(
			JdbcOutboxRepository JdbcOutboxRepository,
			JdbcOutboxMapper jdbcOutboxMapper,

			ConfirmationTokenVerifiedDomainEventSubcriber confirmationTokenVerifiedDomainEventSubcriber,
            UserRegisteredDomainEventSubcriber userRegisteredDomainEventSubcriber,
			UserSoftDeletedDomainEventSubcriber userSoftDeletedDomainEventSubcriber) {


		this.JdbcOutboxRepository = JdbcOutboxRepository;
		this.jdbcOutboxMapper = jdbcOutboxMapper;

		this.confirmationTokenVerifiedDomainEventSubcriber = confirmationTokenVerifiedDomainEventSubcriber;
        this.userRegisteredDomainEventSubcriber = userRegisteredDomainEventSubcriber;
		this.userSoftDeletedDomainEventSubcriber = userSoftDeletedDomainEventSubcriber;
	}


    //SyncDomainEventSubcribers 
	@EventListener 
	public void onConfirmationTokenVerifiedDomainEvent(ConfirmationTokenVerifiedDomainEvent event) {
		confirmationTokenVerifiedDomainEventSubcriber.handle(event);
	}

    @EventListener
	public void onUserRegisteredDomainEvent(UserRegisteredDomainEvent event) {
		log.info("Received event: {}", event.toString());
		userRegisteredDomainEventSubcriber.handle(event);
	}

	    @EventListener
	public void onUserSoftDeletedDomainEvent(UserSoftDeletedDomainEvent event) {
		log.info("Received event: {}", event.toString());
		userSoftDeletedDomainEventSubcriber.handle(event);
	}



    //IntegrationEventSubcribers - Outbox Pattern
	@EventListener 
    public void onIntegrationEvent(IntegrationEventWrapper<?> event) {
		log.info("Received event: {}", event.toString());
        JdbcOutboxRepository.save(jdbcOutboxMapper.integrationEventWrapperToEntity(event));
    }
}
