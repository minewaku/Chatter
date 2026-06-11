package com.minewaku.chatter.message.infrastructure.messaging.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.minewaku.chatter.message.application.messaging.publisher.integration.MessageBroker;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaMessageBroker implements MessageBroker {

    // private final KafkaRouter kafkaRouter;

    @Override
    public void dispatch(IntegrationEventWrapper<?> event) {
        // kafkaRouter.routeAndSend(event);
    }

    @Override
    public void dispatch(List<? extends IntegrationEventWrapper<?>> events) {
        for (IntegrationEventWrapper<?> event : events) {
            // this.dispatch(event);
        }
    }
}