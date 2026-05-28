package com.minewaku.chatter.message.application.messaging.publisher.integration;

import java.util.List;

import com.minewaku.chatter.message.application.messaging.publisher.AsyncEventDispatcher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;

public class IntegrationEventPublisher {
    
    private final AsyncEventDispatcher<IntegrationEventWrapper<?>> asyncDispatcher;
    
    public IntegrationEventPublisher(AsyncEventDispatcher<IntegrationEventWrapper<?>> asyncDispatcher) {
        this.asyncDispatcher = asyncDispatcher;
    }

    public void publish(IntegrationEventWrapper<?> event) {
        asyncDispatcher.dispatch(event);
    }
    
    public void publish(List<? extends IntegrationEventWrapper<?>> events) {
        asyncDispatcher.dispatch(events);
    }
}

