package com.minewaku.chatter.domain.command.confirmation_token;

import java.time.Duration;

import com.minewaku.chatter.domain.value.id.UserId;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record CreateConfirmationTokenCommand(
	@NonNull UserId userId,
	Duration duration
) {}
