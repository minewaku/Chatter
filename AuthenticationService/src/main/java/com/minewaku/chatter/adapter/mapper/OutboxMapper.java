package com.minewaku.chatter.adapter.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;
import com.minewaku.chatter.adapter.messaging.publisher.EnrichedDomainEvent;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.UserRestoredDomainEvent;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.event.dto.CreatedUserDto;
import com.minewaku.chatter.domain.model.User;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class OutboxMapper {

	private final ObjectMapper objectMapper;

	public OutboxMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public JpaOutboxEntity eventToEntity(EnrichedDomainEvent<? extends DomainEvent> event) {
		if (event == null)
			return null;
		JsonNode payload = objectMapper.convertValue(event.getDomainEvent(), JsonNode.class);
		return JpaOutboxEntity.builder()
				.aggregateId(event.getAggregateId())
				.aggregateType(event.getAggregateType())
				.eventType(event.getDomainEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity messageToEntity(String message) {
		if (message == null || message.isBlank())
			return null;
		try {
			return objectMapper.readValue(message, JpaOutboxEntity.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid outbox JSON payload", e);
		}
	}

	public JpaOutboxEntity fromUserCreatedDomainEventToEntity(UserCreatedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event.getCreatedUserDto(), JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getCreatedUserDto().getId().getValue())
				.aggregateType(User.class.getSimpleName())
				.eventType(UserCreatedDomainEvent.class.getSimpleName())
				.payload(payload)
				.build();
	}

	public EnrichedDomainEvent<UserCreatedDomainEvent> toUserCreatedDomainEvent(CreatedUserDto createdUserDto) {
		return EnrichedDomainEvent.<UserCreatedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(createdUserDto.getId().getValue())
				.domainEvent(UserCreatedDomainEvent.builder()
						.createdUserDto(createdUserDto)
						.build())
				.build();
	}

	public EnrichedDomainEvent<AccountVerifiedDomainEvent> toAccountVerifiedDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<AccountVerifiedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(AccountVerifiedDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}

	public EnrichedDomainEvent<UserSoftDeletedDomainEvent> toUserSoftDeletedDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(UserSoftDeletedDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}

	public EnrichedDomainEvent<UserRestoredDomainEvent> toUserRestoredDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<com.minewaku.chatter.domain.event.UserRestoredDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(UserRestoredDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}

	public EnrichedDomainEvent<UserHardDeletedDomainEvent> toUserHardDeletedDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(UserHardDeletedDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}

	public EnrichedDomainEvent<UserLockedDomainEvent> toUserLockedDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<com.minewaku.chatter.domain.event.UserLockedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(UserLockedDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}

	public EnrichedDomainEvent<UserUnlockedDomainEvent> toUserUnlockedDomainEvent(UserId userId) {
		return EnrichedDomainEvent.<UserUnlockedDomainEvent>builder()
				.aggregateType(User.class.getSimpleName())
				.aggregateId(userId.getValue())
				.domainEvent(UserUnlockedDomainEvent.builder()
						.userId(userId)
						.build())
				.build();
	}
}
