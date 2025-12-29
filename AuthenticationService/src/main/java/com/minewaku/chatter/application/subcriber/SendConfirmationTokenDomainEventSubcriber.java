package com.minewaku.chatter.application.subcriber;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;
import com.minewaku.chatter.domain.port.out.service.EmailSender;

public class SendConfirmationTokenDomainEventSubcriber
		implements DomainEventSubscriber<SendConfirmationTokenDomainEvent> {

	private final EmailSender emailSender;

	public SendConfirmationTokenDomainEventSubcriber(
			EmailSender emailSender) {

		this.emailSender = emailSender;
	}

	@Override
	@Transactional
	public void handle(SendConfirmationTokenDomainEvent event) {
		SendConfirmationTokenDomainEvent castedEvent = (SendConfirmationTokenDomainEvent) event;

		String verifyUrl = "http://localhost:5001/auth-service/api/v1/auth/verification/confirm?token="
				+ castedEvent.getConfirmationToken().getToken();

		Map<String, String> templateParameters = Map.of(
				"verifyUrl", verifyUrl,
				"expirationMinutes", String.valueOf(castedEvent.getConfirmationToken().getDuration().toMinutes()));

		String content = emailSender.buildContent(templateParameters, castedEvent.getMailType());
		emailSender.send(castedEvent.getConfirmationToken().getEmail(),
				castedEvent.getSubject(),
				content);
	}
}
