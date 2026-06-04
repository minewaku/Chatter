package com.minewaku.chatter.profile.presentation.messaging.consumer.single;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetAttachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.AssetDetachedIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.DeleteFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.PersistFileStorageIntegrationEvent;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.AssetAttachedIntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.AssetDetachedIntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.DeleteFileStorageIntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.messaging.subcriber.integration.PersistFileStorageIntegrationEventSubscriber;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.CreateProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.SoftDeleteProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.UpdateEnablementUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Enablement;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.model.Username;
import com.minewaku.chatter.profile.domain.sharedkernel.value.DeletionStatus;
import com.minewaku.chatter.profile.presentation.messaging.consumer.single.model.EnablementChangedEventDto;
import com.minewaku.chatter.profile.presentation.messaging.consumer.single.model.UserRegisteredEventDto;
import com.minewaku.chatter.profile.presentation.messaging.consumer.single.model.UserSoftDeletedEventDto;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class SingleKafkaConsumer {

    private final CreateProfileUseCase createProfileUseCase;
    private final SoftDeleteProfileUseCase softDeleteProfileUseCase;
    private final UpdateEnablementUseCase updateEnablementUseCase;

    private final AssetAttachedIntegrationEventSubscriber assetAttachedIntegrationEventSubscriber;
    private final AssetDetachedIntegrationEventSubscriber assetDetachedIntegrationEventSubscriber;

    private final PersistFileStorageIntegrationEventSubscriber persistFileStorageIntegrationEventSubscriber;
    private final DeleteFileStorageIntegrationEventSubscriber deleteFileStorageIntegrationEventSubscriber;

    private final ObjectMapper objectMapper;


    @KafkaListener(
        topics = "dev.shared.event.identityaccess.user", 
        groupId = "dev-com.minewaku.profile.chatter",
        containerFactory = "singleFactory"
    )
    public void consumeUserEvents(
            @Payload String payload, 
            @Header(value = "eventType", required = false) String eventType) {
                
        try {
            if (eventType == null) {
                return;
            }

            switch (eventType) {
                case "UserRegistered": 
                    handleUserRegistered(payload);
                    break;
                case "EmailChanged":
                    // handleEmailChanged(wrapper);
                    break;
                case "UsernameChanged":
                    // handleUsernameChanged(wrapper);
                    break;
                case "EnablementUpdated":
                    handleEnablementChanged(payload);
                    break;
                case "UserSoftDeleted":
                    handleUserSoftDeleted(payload);
                    break;
                default:
                    log.debug("default skip: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Kafka error: {}", eventType, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(
        topics = "dev.internal.event.profile.outbox", 
        groupId = "dev-com.minewaku.profile.chatter.outbox",
        containerFactory = "singleFactory"
    )
    public void consumeOutboxEvents(
            @Payload String payload, 
            @Header(value = "eventType", required = false) String eventType) {
                
        try {
            if (eventType == null) return;

            switch (eventType) {
                case "AssetAttached":
                    AssetAttachedIntegrationEvent assetAttached = objectMapper.readValue(payload, AssetAttachedIntegrationEvent.class);
                    assetAttachedIntegrationEventSubscriber.handle(assetAttached);
                    break;
                case "AssetDetached":
                    AssetDetachedIntegrationEvent assetDetached = objectMapper.readValue(payload, AssetDetachedIntegrationEvent.class);
                    assetDetachedIntegrationEventSubscriber.handle(assetDetached);
                    break;
                case "PersistFileStorage":
                    PersistFileStorageIntegrationEvent persistFile = objectMapper.readValue(payload, PersistFileStorageIntegrationEvent.class);
                    persistFileStorageIntegrationEventSubscriber.handle(persistFile);
                    break;
                case "DeleteFileStorage":
                    DeleteFileStorageIntegrationEvent deleteFile = objectMapper.readValue(payload, DeleteFileStorageIntegrationEvent.class);
                    deleteFileStorageIntegrationEventSubscriber.handle(deleteFile);
                    break;
                default:
                    log.debug("default skip: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Kafka error: {}", eventType, e);
            throw new RuntimeException(e);
        }
    }

    private void handleUserRegistered(String payload) throws Exception {
        UserRegisteredEventDto eventData = objectMapper.readValue(payload, UserRegisteredEventDto.class);
        log.info("handleUserRegistered: {}", eventData);
        CreateProfileUseCase.Command command = new CreateProfileUseCase.Command(
            new ProfileId (eventData.userId()),
            new Username(eventData.username()),
            new Enablement(
                eventData.enabled(),
                eventData.locked(),
                new DeletionStatus(
                    eventData.deleted(),
                    eventData.deletedAt()
                )
            )
        );

        createProfileUseCase.handle(command);
    }

    private void handleUserSoftDeleted(String payload) throws Exception {
        UserSoftDeletedEventDto eventData = objectMapper.readValue(payload, UserSoftDeletedEventDto.class);
        SoftDeleteProfileUseCase.Command command = new SoftDeleteProfileUseCase.Command(
            new ProfileId (eventData.userId()),
            new Username(eventData.username()),
            new Enablement(
                eventData.enabled(),
                eventData.locked(),
                new DeletionStatus(
                    eventData.deleted(),
                    eventData.deletedAt()
                )
            )
        );

        softDeleteProfileUseCase.handle(command);
    }

    private void handleEnablementChanged(String payload) throws Exception {   
        EnablementChangedEventDto eventData = objectMapper.readValue(payload, EnablementChangedEventDto.class);
        log.info("handleUserRegistered: {}", eventData);
        UpdateEnablementUseCase.Command command = new UpdateEnablementUseCase.Command(
            new ProfileId(eventData.userId()),
            new Enablement(
                eventData.enabled(),
                eventData.locked(),
                new DeletionStatus(
                    eventData.deleted(),
                    eventData.deletedAt()
                )
            ));

        updateEnablementUseCase.handle(command);
    }
}