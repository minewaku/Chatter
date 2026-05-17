package com.minewaku.chatter.message.application.messaging.publisher.integration;

import com.minewaku.chatter.message.application.messaging.publisher.AsyncEventDispatcher;
import com.minewaku.chatter.message.application.messaging.publisher.integration.event.IntegrationEventWrapper;

public interface MessageBroker extends AsyncEventDispatcher<IntegrationEventWrapper<?>> {

}
