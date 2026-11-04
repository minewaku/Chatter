package com.minewaku.chatter.profilenext.presentation.account.messaging;

import com.minewaku.chatter.profilenext.profile.api.command.DeleteProfileDataUseCase;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter for the account-deleted notice. A record whose {@code eventType} header names another type is
 * skipped (new types may share the topic); anything else is parsed and handed to the use case. Failures propagate to
 * the container's error handler, which retries or dead-letters them.
 */
@Component
@RequiredArgsConstructor
public class AccountDeletedListener {

    static final String EVENT_TYPE_HEADER = "eventType";
    static final String USER_SOFT_DELETED = "UserSoftDeleted";

    private final DeleteProfileDataUseCase deleteProfileData;

    @KafkaListener(topics = "${profile-next.messaging.account-topic:identityaccess.user.v1}", groupId = "profile-next")
    public void onRecord(ConsumerRecord<String, String> record) {
        String eventType = eventType(record);
        if (eventType != null && !USER_SOFT_DELETED.equals(eventType)) {
            return;
        }
        AccountDeletedMessage message = AccountDeletedMessage.parse(record.value());
        deleteProfileData.handle(
                new DeleteProfileDataUseCase.Command(message.eventId(), message.userIdAsLong(), message.occurredAt()));
    }

    private static String eventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
