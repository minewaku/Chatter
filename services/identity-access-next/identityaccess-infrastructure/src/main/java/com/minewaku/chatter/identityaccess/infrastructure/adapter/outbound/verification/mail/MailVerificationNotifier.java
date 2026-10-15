package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import com.minewaku.chatter.identityaccess.emailverification.internal.port.VerificationNotifier;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends the verification email. Delivery is retried a bounded number of times (1 attempt + 3 retries with
 * exponential backoff). Failures never include the token or the link in their message.
 */
@Component
public class MailVerificationNotifier implements VerificationNotifier {

    private static final String TEMPLATE = "email-verification";
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templates;
    private final VerificationLinkFactory links;
    private final VerificationMailProperties properties;

    MailVerificationNotifier(
            JavaMailSender mailSender,
            ITemplateEngine templates,
            VerificationLinkFactory links,
            VerificationMailProperties properties) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.links = links;
        this.properties = properties;
    }

    @Override
    @Retryable(includes = VerificationMailException.class, maxRetries = 3, delay = 200, multiplier = 2)
    public void sendVerification(Email email, String rawToken, Instant expiresAt) {
        Context context = new Context();
        context.setVariable("link", links.linkFor(rawToken));
        context.setVariable("expiresAt", EXPIRY_FORMAT.format(expiresAt));
        String html = templates.process(TEMPLATE, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(email.value());
            helper.setSubject(properties.subject());
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new VerificationMailException("Could not deliver the verification email", e);
        }
    }
}
