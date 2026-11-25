package com.minewaku.chatter.profile.presentation.account.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.minewaku.chatter.profile.presentation.shared.messaging.UnreadableMessageException;
import java.time.Instant;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

/**
 * This service's own copy of the account-deleted message shape (contracts/account-deleted-event.md). Unknown fields are
 * ignored; anything that cannot be read is an {@link UnreadableMessageException}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountDeletedMessage(String eventId, String userId, Instant occurredAt) {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern POSITIVE_WHOLE_NUMBER = Pattern.compile("[1-9][0-9]*");

    public static AccountDeletedMessage parse(String value) {
        if (value == null || value.isBlank()) {
            throw new UnreadableMessageException("the notice has no value");
        }
        AccountDeletedMessage message;
        try {
            message = JSON.readValue(value, AccountDeletedMessage.class);
        } catch (RuntimeException e) {
            throw new UnreadableMessageException("the notice is not readable JSON of the expected shape");
        }
        if (message == null
                || message.eventId == null
                || message.eventId.isBlank()
                || message.userId == null
                || message.occurredAt == null) {
            throw new UnreadableMessageException("the notice is missing eventId, userId or occurredAt");
        }
        message.userIdAsLong();
        return message;
    }

    public long userIdAsLong() {
        if (!POSITIVE_WHOLE_NUMBER.matcher(userId).matches()) {
            throw new UnreadableMessageException("the notice's userId is not a positive whole number");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UnreadableMessageException("the notice's userId is not a positive whole number");
        }
    }
}
