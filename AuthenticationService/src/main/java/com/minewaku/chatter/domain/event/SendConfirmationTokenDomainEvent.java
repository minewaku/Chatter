package com.minewaku.chatter.domain.event;

import com.minewaku.chatter.domain.event.core.DomainEvent;
import com.minewaku.chatter.domain.model.ConfirmationToken;
import com.minewaku.chatter.domain.value.MailType;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SendConfirmationTokenDomainEvent extends DomainEvent {
	
	@NonNull
	private final ConfirmationToken confirmationToken;
	
	@NonNull
	private final MailType mailType;
	
	private final String subject;
    
    public SendConfirmationTokenDomainEvent(
    		@NonNull ConfirmationToken confirmationToken,
    		@NonNull MailType mailType,
    		String subject) {
    	
    	super();
    	this.confirmationToken = confirmationToken;
    	this.mailType = mailType;
    	this.subject = subject;
    }

}
