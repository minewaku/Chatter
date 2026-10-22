package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import com.minewaku.chatter.identityaccess.infrastructure.integration.resilience.ResilienceConfiguration;
import com.minewaku.chatter.identityaccess.shared.exception.MailDeliveryException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * The one place mail leaves the service. A send is retried a bounded number of times (1 attempt + 3 retries with
 * exponential backoff), and the retried send as a whole is one call through the mail breaker: repeated failures open
 * the breaker, and while it is open nothing is sent and the caller learns at once. Both outcomes surface as
 * {@link MailDeliveryException}, whose message carries no address, token, or link.
 */
@Component
class MailDispatcher {

    private final JavaMailSender sender;
    private final CircuitBreaker breaker;
    private final RetryTemplate retry;

    @Autowired
    MailDispatcher(JavaMailSender sender, @Qualifier(ResilienceConfiguration.MAIL) CircuitBreaker breaker) {
        this(
                sender,
                breaker,
                RetryPolicy.builder()
                        .maxRetries(3)
                        .delay(Duration.ofMillis(200))
                        .multiplier(2)
                        .includes(MailException.class)
                        .build());
    }

    MailDispatcher(JavaMailSender sender, CircuitBreaker breaker, RetryPolicy policy) {
        this.sender = sender;
        this.breaker = breaker;
        this.retry = new RetryTemplate(policy);
    }

    MimeMessage newMessage() {
        return sender.createMimeMessage();
    }

    void send(MimeMessage message) {
        try {
            breaker.executeRunnable(() -> retry.invoke(() -> sender.send(message)));
        } catch (RuntimeException e) {
            // an open breaker (CallNotPermittedException) and a send that failed after its retries look the same
            throw new MailDeliveryException(e);
        }
    }
}
