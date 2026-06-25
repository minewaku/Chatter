package com.minewaku.chatter.profile.application.messaging.publisher.integration.event;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IntegrationEvent {	

	private String eventId;
	private transient final String aggregateType;
	private transient final String eventType;
	
	public IntegrationEvent(String aggregateType, String eventType) {
		this.aggregateType = aggregateType;
		this.eventType = eventType;
	}
}
