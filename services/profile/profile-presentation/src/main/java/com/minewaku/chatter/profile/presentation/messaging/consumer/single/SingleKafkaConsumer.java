package com.minewaku.chatter.profile.presentation.messaging.consumer.single;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SingleKafkaConsumer {

    private final CreateProfileUseCase createProfileUseCase;
    private final SoftDeleteProfileUseCase softDeleteProfileUseCase;
    private final UpdateEnablementUseCase updateEnablementUseCase;
    
    private final ObjectMapper objectMapper;

    public SingleKafkaConsumer(
            CreateProfileUseCase createProfileUseCase, 
            SoftDeleteProfileUseCase softDeleteProfileUseCase,
            UpdateEnablementUseCase updateEnablementUseCase,
            ObjectMapper objectMapper) {

        this.createProfileUseCase = createProfileUseCase;
        this.softDeleteProfileUseCase = softDeleteProfileUseCase;
        this.updateEnablementUseCase = updateEnablementUseCase;
        this.objectMapper = objectMapper;
    }

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


    // @KafkaListener(
    //     topics = "dev.internal.event.profile.outbox", 
    //     groupId = "dev-com.minewaku.profile.chatter.outbox",
    //     containerFactory = "singleFactory"
    // )
    // public void consumeAssetEvents(
    //         @Payload String payload, 
    //         @Header(value = "eventType", required = false) String eventType) {
                
    //     try {
    //         if (eventType == null) {
    //             return;
    //         }

    //         switch (eventType) {
    //             case "AssetDetached": 
    //                 AssetDetachedIntegrationEvent eventData = 
    //                         objectMapper.readValue(payload, AssetDetachedIntegrationEvent.class);
    //                 break;
    //             default:
    //                 log.debug("default skip: {}", eventType);
    //         }

    //     } catch (Exception e) {
    //         log.error("Kafka error: {}", eventType, e);
    //         throw new RuntimeException(e);
    //     }
    // }

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