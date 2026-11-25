package com.minewaku.chatter.profile.presentation.account.messaging;

import com.minewaku.chatter.profile.presentation.shared.messaging.EventHandler;
import com.minewaku.chatter.profile.profile.api.command.DeleteProfileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter for the account-deleted notice ({@code UserSoftDeleted}): parses it and hands it to the use case.
 * Failures propagate to the container's error handler, which retries or dead-letters them.
 */
@Component
@RequiredArgsConstructor
public class AccountDeletedHandler implements EventHandler<AccountDeletedMessage> {

    static final String EVENT_TYPE = "UserSoftDeleted";

    private final DeleteProfileUseCase deleteProfile;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public AccountDeletedMessage parse(String payload) {
        return AccountDeletedMessage.parse(payload);
    }

    @Override
    public void handle(AccountDeletedMessage message) {
        deleteProfile.handle(
                new DeleteProfileUseCase.Command(message.eventId(), message.userIdAsLong(), message.occurredAt()));
    }
}
