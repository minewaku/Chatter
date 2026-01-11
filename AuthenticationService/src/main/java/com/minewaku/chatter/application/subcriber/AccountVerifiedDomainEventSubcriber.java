package com.minewaku.chatter.application.subcriber;

import com.minewaku.chatter.application.publisher.StoreEvent;
import com.minewaku.chatter.application.publisher.StoreEventPublisher;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;

public class AccountVerifiedDomainEventSubcriber implements DomainEventSubscriber<AccountVerifiedDomainEvent> {

	private final StoreEventPublisher storeEventPublisher;

	public AccountVerifiedDomainEventSubcriber(
			StoreEvent storeEvent) {

		this.storeEventPublisher = new StoreEventPublisher(storeEvent);
	}

	@Override
	public void handle(AccountVerifiedDomainEvent event) {
		storeEventPublisher.publish(event);
	}

}
