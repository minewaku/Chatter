package com.minewaku.chatter.profile.application.messaging.publisher.integration;

import com.minewaku.chatter.profile.application.messaging.publisher.AsyncEventDispatcher;
import com.minewaku.chatter.profile.application.messaging.publisher.integration.event.IntegrationEventWrapper;

public interface OutboxStore extends AsyncEventDispatcher<IntegrationEventWrapper<?>> {

}
