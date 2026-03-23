package com.minewaku.chatter.identityaccess.application.messaging.publisher.integration.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@Getter
public class IntegrationEvent {	

	@JsonIgnore
	private final String aggregateType;

	@JsonIgnore
	private final String eventType;
	
	public IntegrationEvent(String aggregateType, String eventType) {
		this.aggregateType = aggregateType;
		this.eventType = eventType;
	}
}
