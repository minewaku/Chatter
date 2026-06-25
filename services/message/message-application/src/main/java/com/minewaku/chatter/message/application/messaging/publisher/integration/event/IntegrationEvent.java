package com.minewaku.chatter.message.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationEvent {	

	private String eventId;
	private transient final String aggregateType;
	private transient final String eventType;
	
	public IntegrationEvent(String aggregateType, String eventType) {
		this.aggregateType = aggregateType;
		this.eventType = eventType;
	}

	
}
