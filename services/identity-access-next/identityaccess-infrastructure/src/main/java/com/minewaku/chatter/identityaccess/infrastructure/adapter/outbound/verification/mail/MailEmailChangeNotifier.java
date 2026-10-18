package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.port.EmailChangeNotifier;
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
 * Sends the email-change confirmation to the new address only. It lives next to the registration notifier because
 * both share the sender, the base URL, the link rules, and the failure type. Delivery is retried a bounded number of
 * times (1 attempt + 3 retries with exponential backoff), and failures never include the token or the link.
 */
@Component
public class MailEmailChangeNotifier implements EmailChangeNotifier {

    private static final String TEMPLATE = "email-change";
    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templates;
    private final VerificationLinkFactory links;
    private final VerificationMailProperties properties;

    MailEmailChangeNotifier(
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
    public void sendEmailChange(Email newEmail, String rawToken, Instant expiresAt) {
        Context context = new Context();
        context.setVariable("link", links.emailChangeLinkFor(rawToken));
        context.setVariable("expiresAt", EXPIRY_FORMAT.format(expiresAt));
        String html = templates.process(TEMPLATE, context);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(newEmail.value());
            helper.setSubject(properties.emailChangeSubject());
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            throw new VerificationMailException("Could not deliver the email change confirmation", e);
        }
    }
}
