package com.minewaku.chatter.application.subcriber;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.domain.event.SendConfirmationTokenDomainEvent;
import com.minewaku.chatter.domain.event.core.DomainEventSubscriber;
import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.port.out.service.UrlGenerator;

import io.github.resilience4j.retry.annotation.Retry;

public class SendConfirmationTokenDomainEventSubcriber
		implements DomainEventSubscriber<SendConfirmationTokenDomainEvent> {

	private final EmailSender emailSender;
	private final UrlGenerator.ConfirmTokenUrlGenerator urlGenerator;

	public SendConfirmationTokenDomainEventSubcriber(
			EmailSender emailSender,
			UrlGenerator.ConfirmTokenUrlGenerator urlGenerator) {
		this.emailSender = emailSender;
		this.urlGenerator = urlGenerator;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(SendConfirmationTokenDomainEvent event) {
		String verifyUrl = urlGenerator
				.generate(event.getConfirmationToken().getToken());

		Map<String, String> templateParameters = Map.of(
				"verifyUrl", verifyUrl,
				"expirationMinutes", String.valueOf(event.getConfirmationToken().getDuration().toMinutes()));
				
		String content = emailSender.buildContent(templateParameters, event.getMailType());
		emailSender.send(event.getConfirmationToken().getEmail(),
				event.getSubject(),
				content);
	}
}
