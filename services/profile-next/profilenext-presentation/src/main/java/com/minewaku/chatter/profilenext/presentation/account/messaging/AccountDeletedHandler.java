package com.minewaku.chatter.profilenext.presentation.account.messaging;

import com.minewaku.chatter.profilenext.presentation.shared.messaging.EventHandler;
import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
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

    private final DeleteProfileDataUseCase deleteProfileData;

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
        deleteProfileData.handle(
                new DeleteProfileDataUseCase.Command(message.eventId(), message.userIdAsLong(), message.occurredAt()));
    }
}
