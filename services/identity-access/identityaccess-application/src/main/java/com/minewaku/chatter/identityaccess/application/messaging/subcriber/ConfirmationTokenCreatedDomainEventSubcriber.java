package com.minewaku.chatter.identityaccess.application.messaging.subcriber;

import java.time.Duration;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import com.minewaku.chatter.identityaccess.application.messaging.subcriber.core.DomainEventSubscriber;
import com.minewaku.chatter.identityaccess.application.port.outbound.notification.EmailSender;
import com.minewaku.chatter.identityaccess.application.port.outbound.provider.UrlGenerator;
import com.minewaku.chatter.identityaccess.domain.aggregate.confirmationtoken.event.ConfirmationTokenCreatedDomainEvent;
import com.minewaku.chatter.identityaccess.domain.aggregate.user.model.Email;
import com.minewaku.chatter.identityaccess.domain.sharedkernel.value.MailType;

import io.github.resilience4j.retry.annotation.Retry;

public class ConfirmationTokenCreatedDomainEventSubcriber implements DomainEventSubscriber<ConfirmationTokenCreatedDomainEvent> {
    
    private final EmailSender emailSender;
    private final UrlGenerator.ConfirmationTokenUrlGenerator urlGenerator;

	public ConfirmationTokenCreatedDomainEventSubcriber(
        EmailSender emailSender,
        UrlGenerator.ConfirmationTokenUrlGenerator urlGenerator) {
        this.emailSender = emailSender;
        this.urlGenerator = urlGenerator;
	}

	@Override
	@Retry(name = "transientDataAccess")
	@Transactional
	public void handle(ConfirmationTokenCreatedDomainEvent event) {

        String verifyUrl = urlGenerator
				.generate(event.getToken());

		Map<String, String> templateParameters = Map.of(
				"verifyUrl", verifyUrl,
				"expirationMinutes", String.valueOf(Duration.parse(event.getDuration()).toMinutes())
            );
				
		String content = emailSender.buildContent(templateParameters, MailType.EMAIL_CONFIRMATION);
		emailSender.send(
                new Email(event.getEmail()),
				"Please confirm your email",
				content);
	}
}
