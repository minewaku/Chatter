package com.minewaku.chatter.application.subcriber;

import java.util.Map;

import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;
import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.port.out.service.LinkGenerator;

public class SendConfirmationTokenDomainEventSubcriber implements DomainEventSubscriber<SendConfirmationTokenDomainEvent> {
	
	private final LinkGenerator linkGenerator;    
    private final EmailSender emailSender;

    public SendConfirmationTokenDomainEventSubcriber (
            LinkGenerator linkGenerator,    
            EmailSender emailSender) {
  
        this.linkGenerator = linkGenerator;
        this.emailSender = emailSender;
    }

	@Override
	public void handle(SendConfirmationTokenDomainEvent event) {
		SendConfirmationTokenDomainEvent castedEvent = (SendConfirmationTokenDomainEvent) event;
		
		Map<String, String> urlParameters = Map.of("token", castedEvent.getConfirmationToken().getToken());
    	String verificationAddress = linkGenerator.generate(event.getMailType(), urlParameters);
    	
    	Map<String, String> templateParameters = Map.of(
    			"verificationAddress", verificationAddress,
    			"token", castedEvent.getConfirmationToken().getToken());
    	
    	String content = emailSender.buildContent(templateParameters, castedEvent.getMailType());
    	emailSender.send(castedEvent.getConfirmationToken().getEmail(),
    			castedEvent.getSubject(),
    			content);
	}
}
