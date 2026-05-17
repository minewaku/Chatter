package com.minewaku.chatter.message.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.messaging.publisher.integration.OutboxStore;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.OutboxJdbcRepository;
import com.minewaku.chatter.message.infrastructure.persistence.postgresql.mapper.OutboxJdbcMapper;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@AllArgsConstructor
public class JdbcOutboxStore implements OutboxStore {

	private final OutboxJdbcRepository outboxJdbcRepository;
    private final OutboxJdbcMapper outboxJdbcMapper;

	@Override
	public void dispatch(IntegrationEventWrapper<?> event) {
		log.info("Saving directly to Outbox: {}", event.getId());
        outboxJdbcRepository.save(outboxJdbcMapper.integrationEventWrapperToEntity(event));
	}

	@Override
	public void dispatch(List<IntegrationEventWrapper<?>> events) {
		log.info("Saving batch of events to Outbox: {}", events.size());
		outboxJdbcRepository.saveAll(events.stream()
			.map(outboxJdbcMapper::integrationEventWrapperToEntity)
			.collect(java.util.stream.Collectors.toList())
		);
	}
}