package com.minewaku.chatter.domain.command.confirmation_token;

import java.time.Duration;

import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CreateConfirmationTokenCommand {
	
	@NonNull
	private final UserId userId;
	
	private final Duration duration;
	
	public CreateConfirmationTokenCommand(@NonNull UserId userId,
			Duration duration) {
		
		this.userId = userId;
		this.duration = duration;
	}
}
