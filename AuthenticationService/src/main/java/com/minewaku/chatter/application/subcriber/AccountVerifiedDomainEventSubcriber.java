package com.minewaku.chatter.application.subcriber;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.publisher.QueueEventPublisher;
import com.minewaku.chatter.domain.event.AccountVerifiedDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;

public class AccountVerifiedDomainEventSubcriber implements DomainEventSubscriber<AccountVerifiedDomainEvent> {

	private final QueueEventPublisher queueEventPublisher;
	
	
	public AccountVerifiedDomainEventSubcriber(
			MessageQueue messageQueue) {
		
		this.queueEventPublisher = new QueueEventPublisher(messageQueue);
	}

	
	@Override
	public void handle(AccountVerifiedDomainEvent event) {
		queueEventPublisher.publish(event);
	} 

}
