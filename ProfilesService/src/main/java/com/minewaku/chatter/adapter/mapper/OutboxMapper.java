package com.minewaku.chatter.adapter.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;
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
		if (message == null || message.isBlank()) {
			return null;
		}

		try {
			JsonNode root = objectMapper.readTree(message);
			JsonNode afterNode = root.path("payload").path("after");

			if (afterNode.isMissingNode() || afterNode.isNull()) {
				return null;
			}

			Long id = afterNode.path("id").asLong();
			String aggregateType = afterNode.path("aggregate_type").asText();
			String aggregateId = afterNode.path("aggregate_id").asText();

			String eventType = null;
			if (afterNode.has("event_type")) {
				eventType = afterNode.path("event_type").asText();
			} else if (afterNode.has("eventType")) {
				eventType = afterNode.path("eventType").asText();
			}

			JsonNode payloadNode = null;
			JsonNode rawPayload = afterNode.path("payload");
			if (rawPayload.isTextual()) {
				payloadNode = objectMapper.readTree(rawPayload.asText());
			} else {
				payloadNode = rawPayload;
			}

			Instant createdAt = null;
			JsonNode createdAtNode = afterNode.path("created_at");
			if (createdAtNode.isTextual()) {
				createdAt = Instant.parse(createdAtNode.asText());
			} else if (createdAtNode.isNumber()) {
				createdAt = Instant.ofEpochMilli(createdAtNode.asLong() / 1000);
			} else {
				createdAt = Instant.now();
			}

			return JpaOutboxEntity.builder()
					.id(id)
					.aggregateType(aggregateType)
					.aggregateId(aggregateId)
					.eventType(eventType)
					.payload(payloadNode)
					.createdAt(createdAt)
					.build();

		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid outbox JSON payload", e);
		}
	}

	public JpaOutboxEntity fromUserCreatedDomainEventToEntity(UserCreatedDomainEvent event) {
		JsonNode payload = objectMapper.convertValue(event.getCreateUserCommand(), JsonNode.class);

		return JpaOutboxEntity.builder()
				.aggregateId(event.getCreateUserCommand().getId().getValue().toString())
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
