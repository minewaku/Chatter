package com.minewaku.chatter.identityaccess.infrastructure.adapter.outbound.verification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minewaku.chatter.identityaccess.infrastructure.integration.mail.MailConfiguration;
import com.minewaku.chatter.identityaccess.user.internal.model.Email;
import com.minewaku.chatter.identityaccess.user.internal.port.EmailChangeNotifier;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(OutputCaptureExtension.class)
class MailEmailChangeNotifierTest {

    private static final String TOKEN = "super-secret-change-token";

    static JavaMailSender sender;

    @Configuration
    @Import(MailConfiguration.class)
    static class Config {

        @Bean
        JavaMailSender mailSender() {
            return sender;
        }

        @Bean
        SpringTemplateEngine templateEngine() {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        VerificationMailProperties properties() {
            return new VerificationMailProperties(
                    "sender@chatter.test",
                    "Please verify",
                    "https://chatter.test",
                    "/confirm",
                    "Confirm the new address",
                    "/confirm-email-change");
        }

        @Bean
        VerificationLinkFactory linkFactory(VerificationMailProperties properties) {
            return new VerificationLinkFactory(properties);
        }

        @Bean
        MailEmailChangeNotifier notifier(
                JavaMailSender mailSender,
                SpringTemplateEngine templates,
                VerificationLinkFactory links,
                VerificationMailProperties properties) {
            return new MailEmailChangeNotifier(mailSender, templates, links, properties);
        }
    }

    private AnnotationConfigApplicationContext context;
    private EmailChangeNotifier notifier;

    @BeforeEach
    void setUp() {
        sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        context = new AnnotationConfigApplicationContext(Config.class);
        notifier = context.getBean(EmailChangeNotifier.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void sendsOneMessageToTheNewAddressOnlyWithTheChangeLink() throws Exception {
        notifier.sendEmailChange(new Email("new@example.com"), TOKEN, Instant.parse("2026-12-24T12:30:00Z"));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(1)).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getFrom()[0].toString()).isEqualTo("sender@chatter.test");
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("new@example.com");
        assertThat(message.getSubject()).isEqualTo("Confirm the new address");
        assertThat(String.valueOf(message.getContent()))
                .contains("https://chatter.test/confirm-email-change?token=" + TOKEN)
                .doesNotContain("https://chatter.test/confirm?token=")
                .contains("2026-12-24 12:30 UTC");
    }

    @Test
    void retriesAndSucceedsAfterTransientFailures() {
        doThrow(new MailSendException("smtp down"))
                .doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(sender)
                .send(any(MimeMessage.class));

        notifier.sendEmailChange(
                new Email("new@example.com"), TOKEN, Instant.now().plusSeconds(60));

        verify(sender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void givesUpAfterFourAttemptsAndNeverExposesTheTokenOrTheLink(CapturedOutput output) {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));

        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> notifier.sendEmailChange(
                new Email("new@example.com"), TOKEN, Instant.now().plusSeconds(60)));

        assertThat(failure).isNotNull();
        verify(sender, times(4)).send(any(MimeMessage.class));
        for (Throwable t = failure; t != null; t = t.getCause()) {
            assertThat(String.valueOf(t.getMessage())).doesNotContain(TOKEN).doesNotContain("chatter.test");
        }
        assertThat(output.getAll()).doesNotContain(TOKEN);
    }

    @Test
    void doesNotRetryWhenTheMessageSucceedsTheFirstTime() {
        doNothing().when(sender).send(any(MimeMessage.class));

        notifier.sendEmailChange(
                new Email("new@example.com"), TOKEN, Instant.now().plusSeconds(60));

        verify(sender, times(1)).send(any(MimeMessage.class));
    }
}
