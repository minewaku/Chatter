package com.minewaku.chatter.adapter.messaging.publisher;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {
	
    private final MessageChannel outboxChannel;

    public OutboxEventPublisher (
        @Qualifier("outboxChannel") MessageChannel outboxChannel
    ) {
        this.outboxChannel = outboxChannel;
    }

    public void publish(String eventJson) {
        outboxChannel.send(MessageBuilder.withPayload(eventJson).build());
    }
}
