package com.minewaku.chatter.domain.command.auth;

import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.value.Email;
import com.minewaku.chatter.domain.value.MailType;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SendConfirmationTokenCommand {
	
	@NonNull
	private final ConfirmationToken confirmationToken;
	
	@NonNull
	private final String verificationPath;
	
	@NonNull
	private final MailType mailType;
	
	@NonNull
	private final Email to;
	
	@NonNull
	private final String subject;
	
	
	public SendConfirmationTokenCommand(@NonNull ConfirmationToken confirmationToken, 
			@NonNull String verificationPath,
			@NonNull MailType mailType,
			@NonNull Email to, 
			@NonNull String subject) {
		
		this.confirmationToken = confirmationToken;
		this.verificationPath = verificationPath;
		this.mailType = mailType;
		this.to = to;
		this.subject = subject;
	}
}
