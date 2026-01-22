package com.minewaku.chatter.adapter.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;
import com.minewaku.chatter.adapter.mapper.OutboxMapper;
import com.minewaku.chatter.adapter.mapper.UserMapper;
import com.minewaku.chatter.adapter.messaging.message.CreateUserMessage;
import com.minewaku.chatter.application.service.user.CreateUserApplicationService;
import com.minewaku.chatter.application.service.user.HardDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.LockUserApplicationService;
import com.minewaku.chatter.application.service.user.RestoreUserApplicationService;
import com.minewaku.chatter.application.service.user.SoftDeleteUserApplicationService;
import com.minewaku.chatter.application.service.user.UnlockUserApplicationService;
import com.minewaku.chatter.domain.command.profile.CreateUserCommand;
import com.minewaku.chatter.domain.value.id.UserId;

@Component
public class OutboxEventConsumer {

	private final OutboxMapper outboxMapper;
	private final UserMapper userMapper;
	private final ObjectMapper objectMapper;
	private final CreateUserApplicationService createUserApplicationService;
	private final HardDeleteUserApplicationService hardDeleteUserApplicationService;
	private final LockUserApplicationService lockUserApplicationService;
	private final RestoreUserApplicationService restoreUserApplicationService;
	private final SoftDeleteUserApplicationService softDeleteUserApplicationService;
	private final UnlockUserApplicationService unlockUserApplicationService;

	public OutboxEventConsumer(
			OutboxMapper outboxMapper,
			UserMapper userMapper,
			ObjectMapper objectMapper,
			CreateUserApplicationService createUserApplicationService,
			HardDeleteUserApplicationService hardDeleteUserApplicationService,
			LockUserApplicationService lockUserApplicationService,
			RestoreUserApplicationService restoreUserApplicationService,
			SoftDeleteUserApplicationService softDeleteUserApplicationService,
			UnlockUserApplicationService unlockUserApplicationService) {

		this.outboxMapper = outboxMapper;
		this.userMapper = userMapper;
		this.objectMapper = objectMapper;
		this.createUserApplicationService = createUserApplicationService;
		this.hardDeleteUserApplicationService = hardDeleteUserApplicationService;
		this.lockUserApplicationService = lockUserApplicationService;
		this.restoreUserApplicationService = restoreUserApplicationService;
		this.softDeleteUserApplicationService = softDeleteUserApplicationService;
		this.unlockUserApplicationService = unlockUserApplicationService;
	}

	@KafkaListener(topics = "dev.shared.entity.authentication.user.id", groupId =
	"dev.com.minewaku.chatter.profile.profile-service.user")
	public void handleUserCdc(String message) throws JsonProcessingException {
		JpaOutboxEntity entity = outboxMapper.messageToEntity(message);
		String eventType = entity.getEventType();

		switch (eventType) {
			case "UserCreated" -> {
				CreateUserMessage payload = objectMapper.treeToValue(entity.getPayload(),
				CreateUserMessage.class);
				CreateUserCommand command = userMapper.CreateUserMessageToCommand(payload);
				createUserApplicationService.handle(command);
			}
			case "UserHardDeleted" -> {
				UserId userId = new UserId(Long.valueOf(entity.getAggregateId()));
				hardDeleteUserApplicationService.handle(userId);
			}
			case "UserSoftDeleted" -> {
				UserId userId = new UserId(Long.valueOf(entity.getAggregateId()));
				softDeleteUserApplicationService.handle(userId);
			}
			case "UserRestored" -> {
				UserId userId = new UserId(Long.valueOf(entity.getAggregateId()));
				restoreUserApplicationService.handle(userId);
			}
			case "UserLocked" -> {
				UserId userId = new UserId(Long.valueOf(entity.getAggregateId()));
				lockUserApplicationService.handle(userId);
			}
			case "UserUnlocked" -> {
				UserId userId = new UserId(Long.valueOf(entity.getAggregateId()));
				unlockUserApplicationService.handle(userId);
			}

			default -> throw new IllegalArgumentException("Unknown event type: " +
			eventType);
		}
	}
}
