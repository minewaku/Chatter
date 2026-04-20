package com.minewaku.chatter.profile.presentation.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minewaku.chatter.profile.application.port.inbound.command.profile.usecase.CreateProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.SoftDeleteProfileUseCase;
import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.UpdateEnablementUseCase;
import com.minewaku.chatter.profile.domain.model.profile.model.Enablement;
import com.minewaku.chatter.profile.domain.model.profile.model.ProfileId;
import com.minewaku.chatter.profile.domain.model.profile.model.Username;
import com.minewaku.chatter.profile.domain.sharedkernel.value.DeletionStatus;
import com.minewaku.chatter.profile.presentation.consumer.model.EnablementChangedEventDto;
import com.minewaku.chatter.profile.presentation.consumer.model.IntegrationEventWrapperDto;
import com.minewaku.chatter.profile.presentation.consumer.model.UserRegisteredEventDto;
import com.minewaku.chatter.profile.presentation.consumer.model.UserSoftDeletedEventDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProfileKafkaConsumer {

    private final CreateProfileUseCase createProfileUseCase;
    private final SoftDeleteProfileUseCase softDeleteProfileUseCase;
    private final UpdateEnablementUseCase updateEnablementUseCase;
    private final ObjectMapper objectMapper;

    public ProfileKafkaConsumer(
            CreateProfileUseCase createProfileUseCase, 
            SoftDeleteProfileUseCase softDeleteProfileUseCase,
            UpdateEnablementUseCase updateEnablementUseCase) {

        this.createProfileUseCase = null;
        this.softDeleteProfileUseCase = null;
        this.updateEnablementUseCase = null;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "dev.shared.event.identityaccess.user", groupId = "dev-com.minewaku.profile.chatter")
    public void consumeUserEvents(String message) {
        try {
            IntegrationEventWrapperDto wrapper = objectMapper.readValue(message, IntegrationEventWrapperDto.class);
            switch (wrapper.eventType()) {
                case "UserRegistered":
                    handleUserRegistered(wrapper);
                    break;
                case "EmailChanged":
                    // handleEmailChanged(wrapper);
                    break;
                case "UsernameChanged":
                    // handleUsernameChanged(wrapper);
                    break;
                case "EnablementChanged":
                    handleEnablementChanged(wrapper);
                    break;
                case "UserSoftDeleted":
                    handleUserSoftDeleted(wrapper);
                    break;
                default:
                    log.debug("Bỏ qua event không liên quan: {}", wrapper.eventType());
            }

        } catch (Exception e) {
            log.error("Lỗi khi xử lý Kafka message: {}", message, e);
        }
    }

    private void handleUserRegistered(IntegrationEventWrapperDto wrapper) throws Exception {
        UserRegisteredEventDto eventData = objectMapper.treeToValue(wrapper.event(), UserRegisteredEventDto.class);
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

    private void handleUserSoftDeleted(IntegrationEventWrapperDto wrapper) throws Exception {
        UserSoftDeletedEventDto eventData = objectMapper.treeToValue(wrapper.event(), UserSoftDeletedEventDto.class);
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

    private void handleEnablementChanged(IntegrationEventWrapperDto wrapper) throws Exception {   
        EnablementChangedEventDto eventData = objectMapper.treeToValue(wrapper.event(), EnablementChangedEventDto.class);
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