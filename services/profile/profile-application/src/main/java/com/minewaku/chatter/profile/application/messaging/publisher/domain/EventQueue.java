package com.minewaku.chatter.profile.application.messaging.publisher.domain;

import com.minewaku.chatter.profile.application.messaging.publisher.SyncEventDispatcher;
import com.minewaku.chatter.profile.domain.sharedkernel.event.DomainEvent;

public interface EventQueue extends SyncEventDispatcher<DomainEvent> {

}
