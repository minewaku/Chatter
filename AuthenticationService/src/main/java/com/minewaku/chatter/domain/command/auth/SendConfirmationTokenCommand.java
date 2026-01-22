package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.MailType;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record SendConfirmationTokenCommand(
	@NonNull ConfirmationToken confirmationToken, 
	@NonNull String verificationPath,
	@NonNull MailType mailType,
	@NonNull Email to, 
	@NonNull String subject
) {}
