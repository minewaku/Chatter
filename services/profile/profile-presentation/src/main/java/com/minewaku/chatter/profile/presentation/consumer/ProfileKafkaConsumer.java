package com.minewaku.chatter.profile.presentation.consumer;

import com.minewaku.chatter.profile.application.port.inbound.command.security.usecase.EnableProfileUseCase;

public class ProfileKafkaConsumer {

    private final EnableProfileUseCase enabledProfileUseCase;

    public ProfileKafkaConsumer(
            EnableProfileUseCase enabledProfileUseCase) {

        this.enabledProfileUseCase = enabledProfileUseCase;
    }


    // @KafkaListener(topics = "user-created-topic", groupId = "profile-service-group")
    // public void consumeUserEnabledEvent(String message) {
    //     // Parse the message and extract necessary information
    //     // For example, you might extract the profileId from the message
    //     String profileId = extractProfileIdFromMessage(message);

    //     // Call the use case to enable the profile
    //     enabledProfileUseCase.handle(profileId);
    // }
}
