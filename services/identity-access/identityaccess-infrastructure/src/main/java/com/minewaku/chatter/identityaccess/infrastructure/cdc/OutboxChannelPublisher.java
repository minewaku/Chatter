package com.minewaku.chatter.identityaccess.infrastructure.cdc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

@Component
public class OutboxChannelPublisher {
	
    private final MessageChannel outboxChannel;

    public OutboxChannelPublisher (
        @Qualifier("outboxChannel") MessageChannel outboxChannel
    ) {
        this.outboxChannel = outboxChannel;
    }

    public void publish(String eventJson) {
        outboxChannel.send(MessageBuilder.withPayload(eventJson).build());
    }
}
