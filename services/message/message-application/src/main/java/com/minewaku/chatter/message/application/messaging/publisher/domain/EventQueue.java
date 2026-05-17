package com.minewaku.chatter.message.application.messaging.publisher.domain;

import com.minewaku.chatter.message.application.messaging.publisher.EventDispatcher;
import com.minewaku.chatter.message.domain.sharedkernel.event.DomainEvent;

public interface EventQueue extends EventDispatcher<DomainEvent> {
}
