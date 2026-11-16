package com.minewaku.chatter.profilenext.presentation.account.messaging;

import com.minewaku.chatter.profilenext.presentation.shared.messaging.EventDispatcher;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reads the account topic and hands every record to the dispatcher. The topic is set only in
 * {@code profile-next.messaging.account-topic}; there is no default here, so a missing setting stops start-up.
 */
@Component
@RequiredArgsConstructor
public class AccountTopicListener {

    private final EventDispatcher dispatcher;

    @KafkaListener(topics = "${profile-next.messaging.account-topic}", groupId = "profile-next")
    public void onRecord(ConsumerRecord<String, String> record) {
        dispatcher.dispatch(record);
    }
}
