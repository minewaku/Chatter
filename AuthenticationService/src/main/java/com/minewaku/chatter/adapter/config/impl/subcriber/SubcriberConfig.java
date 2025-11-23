package com.minewaku.chatter.adapter.config.impl.subcriber;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minewaku.chatter.application.publisher.MessageQueue;
import com.minewaku.chatter.application.subcriber.AccountVerifiedDomainEventSubcriber;
import com.minewaku.chatter.application.subcriber.CreateConfirmationTokenDomainEventSubcriber;
import com.minewaku.chatter.application.subcriber.SendConfirmationTokenDomainEventSubcriber;
import com.minewaku.chatter.domain.port.out.repository.ConfirmationTokenRepository;
import com.minewaku.chatter.domain.port.out.repository.UserRepository;
import com.minewaku.chatter.domain.port.out.service.EmailSender;
import com.minewaku.chatter.domain.port.out.service.KeyGenerator;
import com.minewaku.chatter.domain.port.out.service.LinkGenerator;

@Configuration
public class SubcriberConfig {

	@Bean
	public CreateConfirmationTokenDomainEventSubcriber createConfirmationTokenDomainEventSubcriber(
		ConfirmationTokenRepository confirmationTokenRepository,
		UserRepository userRepository,
		KeyGenerator keyGenerator,
		MessageQueue messageQueue
	) {
		return new CreateConfirmationTokenDomainEventSubcriber(
				confirmationTokenRepository,
				userRepository,
				keyGenerator,
				messageQueue);
	}

	@Bean
	public SendConfirmationTokenDomainEventSubcriber sendConfirmationTokenDomainEventSubcriber(
		LinkGenerator linkGenerator,
		EmailSender emailSender
	) {
		return new SendConfirmationTokenDomainEventSubcriber(linkGenerator, emailSender);
	}
	
	@Bean
	public AccountVerifiedDomainEventSubcriber accountVerifiedDomainEventSubcriber(
		MessageQueue messageQueue
	) {
		return new AccountVerifiedDomainEventSubcriber(messageQueue);
	}
}

