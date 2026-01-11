package com.minewaku.chatter.domain.event;

import java.time.Duration;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class CreateConfirmationTokenDomainEvent extends DomainEvent {
	
	@NonNull
	private final UserId userId;
	
	private final Duration duration;
	
	public CreateConfirmationTokenDomainEvent(@NonNull UserId userId,
			Duration duration) {
		super("Created");
		this.userId = userId;
		this.duration = duration;
	}

}
