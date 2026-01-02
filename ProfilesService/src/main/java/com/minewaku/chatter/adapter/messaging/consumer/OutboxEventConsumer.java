package com.minewaku.chatter.adapter.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.adapter.entity.JpaOutboxEntity;
import com.minewaku.chatter.adapter.mapper.OutboxMapper;
import com.minewaku.chatter.application.service.role.CreateRoleApplicationService;
import com.minewaku.chatter.application.service.role.HardDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.RestoreRoleApplicationService;
import com.minewaku.chatter.application.service.role.SoftDeleteRoleApplicationService;
import com.minewaku.chatter.application.service.role.UpdateRoleApplicationService;
import com.minewaku.chatter.application.service.user_role.AssignRoleApplicationService;
import com.minewaku.chatter.application.service.user_role.UnassignRoleApplicationService;
import com.minewaku.chatter.domain.command.role.CreateRoleCommand;
import com.minewaku.chatter.domain.command.role.UpdateRoleCommand;
import com.minewaku.chatter.domain.command.user_role.CreateUserRoleCommand;
import com.minewaku.chatter.domain.value.id.RoleId;
import com.minewaku.chatter.domain.value.id.UserRoleId;

@Component
public class OutboxEventConsumer {

	// deprecated, authentication service now turn into an IDP which also manages
	// roles and user-roles
	// keep as an example of consuming CDC events via Kafka Listener
	private final OutboxMapper outboxMapper;
	private final ObjectMapper objectMapper;
	private final CreateRoleApplicationService createRoleApplicationService;
	private final UpdateRoleApplicationService updateRoleApplicationService;
	private final SoftDeleteRoleApplicationService softDeleteRoleApplicationService;
	private final HardDeleteRoleApplicationService hardDeleteRoleApplicationService;
	private final RestoreRoleApplicationService restoreRoleApplicationService;
	private final AssignRoleApplicationService assignRoleApplicationService;
	private final UnassignRoleApplicationService unassignRoleApplicationService;

	public OutboxEventConsumer(
			OutboxMapper outboxMapper,
			ObjectMapper objectMapper,
			CreateRoleApplicationService createRoleApplicationService,
			UpdateRoleApplicationService updateRoleApplicationService,
			SoftDeleteRoleApplicationService softDeleteRoleApplicationService,
			HardDeleteRoleApplicationService hardDeleteRoleApplicationService,
			RestoreRoleApplicationService restoreRoleApplicationService,
			AssignRoleApplicationService assignRoleApplicationService,
			UnassignRoleApplicationService unassignRoleApplicationService) {
		
		this.outboxMapper = outboxMapper;
		this.objectMapper = objectMapper;
		this.createRoleApplicationService = createRoleApplicationService;
		this.updateRoleApplicationService = updateRoleApplicationService;
		this.softDeleteRoleApplicationService = softDeleteRoleApplicationService;
		this.hardDeleteRoleApplicationService = hardDeleteRoleApplicationService;
		this.restoreRoleApplicationService = restoreRoleApplicationService;
		this.assignRoleApplicationService = assignRoleApplicationService;
		this.unassignRoleApplicationService = unassignRoleApplicationService;
	}

	@KafkaListener(topics = "dev.shared.cdc.authorization.role.id", groupId =
	"dev.com.minewaku.chatter.authorization.authorization-service.role")
	public void handleCdcRole(String message) throws JsonProcessingException {
	JpaOutboxEntity entity = outboxMapper.messageToEntity(message);
	String eventType = entity.getEventType();

	switch (eventType) {
		case "UserCreatedDomainEvent" -> {
		CreateRoleCommand command = objectMapper.treeToValue(entity.getPayload(),
		CreateRoleCommand.class);
		createRoleApplicationService.handle(command);
		}
		case "ROLE_UPDATED" -> {
		UpdateRoleCommand command = objectMapper.treeToValue(entity.getPayload(),
		UpdateRoleCommand.class);
		updateRoleApplicationService.handle(command);
		}
		case "ROLE_SOFT_DELETED" -> {
		RoleId roleId = objectMapper.treeToValue(entity.getPayload(), RoleId.class);
		softDeleteRoleApplicationService.handle(roleId);
		}
		case "ROLE_RESTORED" -> {
		RoleId roleId = objectMapper.treeToValue(entity.getPayload(), RoleId.class);
		restoreRoleApplicationService.handle(roleId);
		}
		case "ROLE_HARD_DELETED" -> {
		RoleId roleId = objectMapper.treeToValue(entity.getPayload(), RoleId.class);
		hardDeleteRoleApplicationService.handle(roleId);
		}

		default -> throw new IllegalArgumentException("Unknown event type: " +
		eventType);
		}
		}

		@KafkaListener(topics = "dev.shared.cdc.authorization.user.role.id", groupId
		= "dev.com.minewaku.chatter.authorization.authorization-service.user.role")
		public void handleCdcUserRole(String message) throws JsonProcessingException
		{
		JpaOutboxEntity entity = outboxMapper.messageToEntity(message);
		String eventType = entity.getEventType();

		switch (eventType) {
		case "USER_ROLE_CREATED" -> {
		CreateUserRoleCommand command = objectMapper.treeToValue(entity.getPayload(),
		CreateUserRoleCommand.class);
		assignRoleApplicationService.handle(command);
		}

		case "USER_ROLE_DELETED" -> {
		UserRoleId command = objectMapper.treeToValue(entity.getPayload(),
		UserRoleId.class);
		unassignRoleApplicationService.handle(command);
		}

		default -> throw new IllegalArgumentException("Unknown event type: " +
		eventType);
		}
	}
}
