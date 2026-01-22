package com.minewaku.chatter.adapter.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;
import com.minewaku.chatter.adapter.messaging.message.CreateUserMessage;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.UserCreatedDomainEvent;
import com.minewaku.chatter.domain.event.UserHardDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserLockedDomainEvent;
import com.minewaku.chatter.domain.event.UserRestoredDomainEvent;
import com.minewaku.chatter.domain.event.UserSoftDeletedDomainEvent;
import com.minewaku.chatter.domain.event.UserUnlockedDomainEvent;
import com.minewaku.chatter.domain.model.User;

@Component
public class OutboxMapper {

	private final ObjectMapper objectMapper;

	public OutboxMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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
		
		CreateUserMessage createUserMessage = new CreateUserMessage(
				event.getId().getValue(),
				event.getEmail().getValue(),
				event.getUsername().getValue(),
				event.getBirthday().getValue().toString(),
				event.isEnabled(),
				event.isLocked(),
				event.isDeleted(),
				event.getDeletedAt().toString(),
				event.getAuditMetadata().getCreatedAt().toString(),
				event.getAuditMetadata().getModifiedAt().toString()
		);
		
		JsonNode payload = objectMapper.convertValue(createUserMessage, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromAccountVerifiedDomainEventToEntity(AccountVerifiedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromUserSoftDeletedDomainEventToEntity(UserSoftDeletedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromUserRestoredDomainEventToEntity(UserRestoredDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromUserHardDeletedDomainEventToEntity(UserHardDeletedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromUserLockedDomainEventToEntity(UserLockedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}

	public JpaOutboxEntity fromUserUnlockedDomainEventToEntity(UserUnlockedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event, JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getUserId().getValue().toString())
				.aggregateType(User.class.getSimpleName())
				.eventType(event.getEventType())
				.payload(payload)
				.build();
	}
}
