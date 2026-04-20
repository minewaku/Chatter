package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.Getter;

@Getter
public class IntegrationEvent {	

	private transient final String aggregateType;
	private transient final String eventType;
	
	public IntegrationEvent(String aggregateType, String eventType) {
		this.aggregateType = aggregateType;
		this.eventType = eventType;
	}

	
}
