package com.minewaku.chatter.profile.presentation.account.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.minewaku.chatter.profile.presentation.shared.messaging.UnreadableMessageException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountDeletedMessageTest {

    @Test
    void parsesTheContractShape() {
        var message = AccountDeletedMessage.parse(
                "{\"eventId\":\"0197aaaa\",\"userId\":\"123456789\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}");

        assertThat(message.eventId()).isEqualTo("0197aaaa");
        assertThat(message.userIdAsLong()).isEqualTo(123456789L);
        assertThat(message.occurredAt()).isEqualTo(Instant.parse("2026-10-29T08:15:30Z"));
    }

    @Test
    void ignoresUnknownFields() {
        var message = AccountDeletedMessage.parse(
                "{\"eventId\":\"e\",\"userId\":\"5\",\"occurredAt\":\"2026-10-29T08:15:30Z\",\"future\":{\"a\":1}}");

        assertThat(message.userIdAsLong()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"userId\":\"5\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"userId\":\"5\"}",
        "{\"eventId\":\"e\",\"userId\":\"abc\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"userId\":\"0\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"userId\":\"-5\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"userId\":\"99999999999999999999\",\"occurredAt\":\"2026-10-29T08:15:30Z\"}",
        "{\"eventId\":\"e\",\"userId\":\"5\",\"occurredAt\":\"yesterday\"}",
        "{\"nonsense\":true}",
        "not json",
        "",
        "[]"
    })
    void anythingUnreadableIsANonRetryableFailure(String value) {
        assertThatThrownBy(() -> AccountDeletedMessage.parse(value)).isInstanceOf(UnreadableMessageException.class);
    }

    @Test
    void aMissingValueIsUnreadable() {
        assertThatThrownBy(() -> AccountDeletedMessage.parse(null)).isInstanceOf(UnreadableMessageException.class);
    }
}
